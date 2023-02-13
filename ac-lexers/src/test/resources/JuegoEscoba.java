import eps.cartas.*;

import java.util.ArrayList;
import java.util.StringTokenizer;

/**
 * {@link Juego} para jugar a la escoba. El objetivo es sumar bazas en las que
 * el valor de las cartas sume 15, especialmente cuando al llevarse la baza
 * se deja la mesa vacía ("hacer escoba"). Las reglas están disponibles en
 * <a href="file:///home/mfreire/pract/poo_0506/p1/escoba.html">la página
 * de la asignatura</a>.
 *
 * @author mfreire
 */
public class JuegoEscoba extends Juego {
        
    private static final int PUNTOS_VICTORIA_DEFECTO = 11;
    
    /** cartas en la mesa, vistas */
    private Mazo cartasEnMesa;
    /** cartas en la mesa, ocultas */
    private Mazo cartasPorRepartir;
    /** cada jugador tiene cartas en su mano, en un monton, y un numero de escobas */
    private DatosJugador[] datosJugador;
    /** ultimo jugador que se hizo baza (se lleva las cartas de la mesa al final) */
    private int ultimoJugadorConBaza;
    /** número de puntos necesarios para ganar; por defecto, PUNTOS_VICTORIA_DEFECTO */
    public int puntosVictoria = PUNTOS_VICTORIA_DEFECTO;
    
    /**
     * Constructor. No hace nada, salvo decir que hasta que se inicialize
     * todo, no se puede jugar.
     */
    public JuegoEscoba() {
        setEstado(INVALIDO);
    }

    /**
     * Inicializa un juego de Escoba, con el numero de jugadores que
     * sea. El jugador 0 reparte, el 1 empieza a jugar.
     * @param numJugadores jugadores que participan; si es 0, se lee de la descripcion
     */
    public void inicializa(int numJugadores) {
        if (numJugadores < 2 || numJugadores > 4) {                
            throw new IllegalArgumentException(
                    "Imposible jugar a la escoba con " + numJugadores + 
                    "jugadores."
            );        
            // despues de lanzar una excepcion: aqui no se llega nunca
        }
       
        this.numJugadores = numJugadores;

        // reserva estructuras para este numero de jugadores
        datosJugador = new DatosJugador[numJugadores];

        // limpia todo, mesa incluida
        for (int i=0; i<numJugadores; i++) {
            datosJugador[i] = new DatosJugador();
        }
        cartasEnMesa = new Mazo();

        // decide quién reparte; empieza el jugador siguiente
        int r = (int)(Math.random()*numJugadores);
        turno = (r + 1) % numJugadores;

        // es posible, jugando mal, no sumar nunca 15; ventaja al que reparte
        ultimoJugadorConBaza = r;

        // reparte cartas para la nueva ronda; puede que el que reparte se haga
        // alguna escoba.
        nuevaRonda();
        
        // declara la partida inicializada
        setEstado(EN_CURSO);
        
        // lee el comentario de 'getJugadasValidas', y decide si descomentas o no...
        // generaValidas();
    }
    
    /**
     * Inicializa un juego de Escoba, a partir de una cadena que lo describe.
     * Esta cadena debe de estar en el mismo formato que genera toString.
     *
     * Formato:
     * <pre>
     *   numJugadores
     *   cartasEnMesa
     *   cartasPorRepartir
     *   datosJugador0
     *   ...
     *   datosJugadorN
     *   turno
     *   ultimoJugadorConBaza
     *   estado
     *   puntosVictoria
     * </pre>
     * @param descripcion una descripcion de la partida
     */
    public void inicializa(String descripcion) {
        StringTokenizer st = new StringTokenizer(descripcion, "\r\n");
        
        // obtiene el numero de jugadores
        this.numJugadores = Integer.parseInt(st.nextToken());
        if (numJugadores < 2 || numJugadores > 4) {                
            throw new IllegalArgumentException(
                    "Imposible jugar bien a la escoba con " + numJugadores + 
                    "jugadores."
            );        
            // despues de lanzar una excepcion: aqui no se llega nunca
        }
        
        // reserva estructuras para este numero de jugadores
        datosJugador = new DatosJugador[numJugadores];
        
        // cartas en la mesa y por repartir
        cartasEnMesa = new Mazo(st.nextToken(), BarajaEsp.getInstance());
        cartasPorRepartir = new Mazo(st.nextToken(), BarajaEsp.getInstance());
        
        // cartas y bazas y escobas de cada jugador
        for (int i=0; i<numJugadores; i++) {
            datosJugador[i] = new DatosJugador(st.nextToken());
        }
        
        // turno, ultimo jugador con baza, y estado
        turno = Integer.parseInt(st.nextToken());
        ultimoJugadorConBaza = Integer.parseInt(st.nextToken());
        estado = Integer.parseInt(st.nextToken());
        puntosVictoria = Integer.parseInt(st.nextToken());
        
        // lee el comentario de 'getJugadasValidas', y decide si descomentas o no...
        // generaValidas();
    }

    /**
     * Limpia los contadores de una ronda y realiza el reparto de cartas para
     * empezar la siguiente.
     * <p>
     * El postre (es decir, el jugador anterior al que tiene el turno) es
     * el que realiza el reparto, incluidas las 4 cartas iniciales. 
     * Si los valores de estas cartas 4 dan para 1 ó 2 escobas, el postre se las
     * apunta.
     */
     void nuevaRonda() {

        // reune el mazo y lo mezcla bien
        cartasPorRepartir = BarajaEsp.getInstance().generaMazo();
        cartasPorRepartir.mezcla();
         
        // limpia los contadores y reparte 3 cartas a cada cual
        for (int i=0; i<numJugadores; i++) {
            DatosJugador dj = datosJugador[i];
            dj.getEnBazas().clear();
            dj.setEscobas(0);
            dj.getEnMano().add(cartasPorRepartir.sacaPrimera());
            dj.getEnMano().add(cartasPorRepartir.sacaPrimera());
            dj.getEnMano().add(cartasPorRepartir.sacaPrimera());
        }

        // pone 4 cartas en la mesa, y va sumando su valor, por si es escoba
        int suma = 0;
        for (int i=0; i<4; i++) {
            Carta sacada = cartasPorRepartir.sacaPrimera();
            cartasEnMesa.add(sacada);
            suma += valor(sacada);
        }
        
        // las cartas las reparte el "postre"
        int r = (turno+numJugadores-1) % numJugadores;

        // si las 4 suman 15, es una escoba para el que reparte
        if (suma == 15 || suma == 30) {
            datosJugador[r].getEnBazas().addAll(cartasEnMesa);
            cartasEnMesa.clear();
            datosJugador[r].setEscobas(suma / 15);
            ultimoJugadorConBaza = r;
        }        
     }
    
    /**
     * Devuelve el valor-escoba de una carta, que es igual que el valor numerico
     * para cartas <= 7, 8 para sotas, 9 para caballos, y 10 para reyes
     * @param carta la Carta cuyo valor se quiere consultar
     * @return el valor de la carta
     */
    public static int valor(Carta carta) {
        switch (BarajaEsp.numeroDeCarta(carta)) {
            case BarajaEsp.AS: return 1;
            case BarajaEsp.DOS: return 2;
            case BarajaEsp.TRES: return 3;
            case BarajaEsp.CUATRO: return 4;
            case BarajaEsp.CINCO: return 5;
            case BarajaEsp.SEIS: return 6;
            case BarajaEsp.SIETE: return 7;
            case BarajaEsp.SOTA: return 8;
            case BarajaEsp.CABALLO: return 9;
            case BarajaEsp.REY: return 10;
        }
        throw new IllegalArgumentException("Carta no reconocida");
    }

    /**
     * Genera una cadena que describe el estado del juego actual, tal y como la
     * espera inicializa(String)
     * @return la representacion
     */
    public String toString() {
        StringBuffer sb = new StringBuffer();
        
        // numero de jugadores 
        sb.append(numJugadores + "\n");

        // mazo de cartas en mesa y por repartir        
        sb.append(cartasEnMesa + "\n" + cartasPorRepartir + "\n");
        
        // mazos de los jugadores, y las escobas que lleva cada cual
        for (int i=0; i<numJugadores; i++) {
            sb.append(datosJugador[i] + "\n");
        }
        
        // turno, ultimo con baza, estado y puntos para victoria
        sb.append(turno + "\n" + ultimoJugadorConBaza + "\n" + estado + "\n");
        sb.append(puntosVictoria + "\n");
        
        return sb.toString();
    }

    /**
     * Crea una jugada para este tipo de juego a partir de su descripcion.
     * Permite crear jugadas para el juego a partir de historiales de movimiento,
     * etcetera, sin tener que saber nada del juego en si.
     * @param descripcion una cadena que describe la jugada, en el formato 
     * en el que las jugadas de este juego se imprimen a si mismas en su 
     * "toString()"
     * @return la jugada descrita en 'descripcion'
     */
    public Jugada creaJugada(String descripcion) {
        return new JugadaEscoba(descripcion);
    }            
    
    /**
     * Devuelve la puntuacion para un jugador de este juego. Si la partida
     * no ha acabado, puede ser aproximada o directamente se puede devolver 0.
     * Si el juego tiene <code>estado == FINALIZADO</code>, entonces debe ser correcta.
     * Para juegos por equipos, lo suyo es devolver lo mismo para todos los
     * jugadores de un equipo.
     * @param i número del jugador cuya puntuación se desea consultar
     * @return la puntuación de ese jugador
     */
    public int getPuntos(int i) {
        return datosJugador[i].getPuntos();
    }    
    
    // ------- funciones no implementadas -------

    
    /**
     * Devuelve una lista con todas las jugadas actualmente validas en el
     * juego. Los elementos de la lista deben ser subclases de "Jugada".
     * @param nJugador numero del jugador para el cual se quieren mirar las jugadas     
     * @return lista con todas las jugadas actualmente validas
     */
    public ArrayList getJugadasValidas(int nJugador) {
        ArrayList <JugadaEscoba> jugadas_validas = new ArrayList();
        ArrayList <Mazo> lista_bazas = new ArrayList();
        
        for(int i=0; i<datosJugador[nJugador].getEnMano().size(); i++){
                jugadas_validas.add(new JugadaEscoba(nJugador, datosJugador[nJugador].getEnMano().cartaEn(i), new Mazo()));
                lista_bazas=BuscadorDeBazas.buscaBazas(datosJugador[nJugador].getEnMano().cartaEn(i), cartasEnMesa);
                for (int j=0; j<lista_bazas.size(); j++){
                    jugadas_validas.add(new JugadaEscoba(nJugador, datosJugador[nJugador].getEnMano().cartaEn(i), lista_bazas.get(j)));
                }                    
        }        
        return jugadas_validas;
    }
    
    /**
     * Devuelve 'true' si una jugada es valida (que es lo mismo que decir
     * que esta dentro de getJugadasValidas).
     * @param j jugada a comprobar
     * @return true si es valida, false si no
     */
    public boolean esJugadaValida(Jugada j) {        
        ArrayList <JugadaEscoba> jugadas_validas = new ArrayList ();
        boolean flag = false;
        
        jugadas_validas=getJugadasValidas(j.getNumJugador());
        for (int i=0; i<jugadas_validas.size(); i++){
            if (j.equals(jugadas_validas.get(i))){
                flag=true;
            }
        }             
        return flag;
    }
    
    /**
     * Realiza una jugada sobre el juego. Despues de realizarla, se habrán
     * realizado todos los cambios correspondientes en el juego, incluyendo,
     * si corresponde, cambio de turno, actualizacion de puntuaciones, 
     * reparto de más cartas, etcétera.
     * @param j jugada a realizar (debe ser valida)
     */
    public void juega(Jugada j) {
        // A RELLENAR
        //   los pasos son:
        //   0- comprobar que 'j' esValida(); si no lo es, lanzar excepcion:
        //      throw new IllegalArgumentException("Jugada no valida: "+j);
        //   1- realizar la jugada: mover cartas de un mazo a otro
        //   2- si es 'escoba', suma 1 a las escobas del jugador actual
        //   3- cambiar el turno
        //   4- si el que tiene que jugar no tiene cartas
        //      - si quedan cartas en el mazo, repartir para otra vuelta
        //      - si no quedan, 
        //        + actualizar puntuaciones de los participantes
        //        + si alguno gana, fin de partida
        //        + si no, empieza nueva ronda, llamando a nuevaRonda().
        //   5- si has implementado 'generaValidas', debes llamarlo aqui
        
        JugadaEscoba je = new JugadaEscoba (j.toString());
        int max=0;
        int max_jugador=-1;
        int numero_jugadores_con_maxima_puntuacion=0;
        
        if(esJugadaValida(j)){
            cartasEnMesa.add(je.getPone());
            datosJugador[je.getNumJugador()].getEnBazas().addAll(je.getQuita());            
            datosJugador[je.getNumJugador()].getEnMano().remove(je.getPone());
            cartasEnMesa.removeAll(je.getQuita());
            if (cartasEnMesa.isEmpty()){
                datosJugador[je.getNumJugador()].setEscobas((datosJugador[je.getNumJugador()].getEscobas())+1);
            }
            if (je.getQuita().isEmpty()==false){
                ultimoJugadorConBaza=je.getNumJugador();
            }
            turno = (turno + 1) % numJugadores;
            if (datosJugador[turno].getEnMano().isEmpty()){
                if (cartasPorRepartir.isEmpty()==false){
                    for (int i=0; i<numJugadores; i++) {
                        datosJugador[i].getEnMano().add(cartasPorRepartir.sacaPrimera());
                        datosJugador[i].getEnMano().add(cartasPorRepartir.sacaPrimera());
                        datosJugador[i].getEnMano().add(cartasPorRepartir.sacaPrimera());
                    }
                }
                else{
                    datosJugador[ultimoJugadorConBaza].getEnBazas().addAll(cartasEnMesa);
                    cartasEnMesa.removeAll(cartasEnMesa);      
                    for (int i=0; i<numJugadores; i++){
                        datosJugador[i].setPuntos((datosJugador[i].getPuntos())+(puntosEnRonda(i)));
                        if (datosJugador[i].getPuntos()>max){
                            max=datosJugador[i].getPuntos();
                            max_jugador=i;
                        }
                    }
                    if (max<PUNTOS_VICTORIA_DEFECTO){
                        nuevaRonda();
                    }
                    else{
                        for (int i=0; i<numJugadores; i++){
                            if (datosJugador[i].getPuntos()==max){
                                numero_jugadores_con_maxima_puntuacion++;
                            }
                        }
                        if (numero_jugadores_con_maxima_puntuacion!=1){
                            nuevaRonda();
                        }
                        else{
                            System.out.println("Partida finalizada. Gana el jugador " + max_jugador + ".");
                            setEstado(FINALIZADO);
                        }
                    }
                }
            }
        }
        else{
            throw new IllegalArgumentException("Jugada no valida: " +j);
        }
    }

    /**
     * Devuelve los puntos de este jugador en la ronda actual. Se debe llamar
     * desde <code>juega</code> cuando se detecta final de ronda, para
     * actualizar los totales de cada jugador.
     * @param i número del jugador cuya puntuación se desea consultar
     * @return la puntuación de ese jugador en esta ronda
     */
    private int puntosEnRonda(int i) {   
        boolean flag_cartas=true;
        boolean flag_oros=true;
        boolean flag_sietes=true;
        int ioros=0;
        int joros=0;
        int isietes=0;
        int jsietes=0;
        int puntos=0;
        
        for (int j=0; j<datosJugador[i].getEnBazas().size();j++){
            if (BarajaEsp.paloDeCarta(datosJugador[i].getEnBazas().cartaEn(j))=='O'){
                ioros++;
            }
            if (BarajaEsp.numeroDeCarta(datosJugador[i].getEnBazas().cartaEn(j))=='7'){
                isietes++;
            }
            if ((BarajaEsp.numeroDeCarta(datosJugador[i].getEnBazas().cartaEn(j))=='7')&&(BarajaEsp.paloDeCarta(datosJugador[i].getEnBazas().cartaEn(j))=='O')){
                puntos++;
            }
        }
        for (int j=0;j<numJugadores;j++){
            joros=0;
            jsietes=0;
            if (j!=i){
                if (datosJugador[j].getEnBazas().size()>datosJugador[i].getEnBazas().size()){
                    flag_cartas=false;
                }
                for (int k=0; k<datosJugador[j].getEnBazas().size(); k++){
                    if (BarajaEsp.paloDeCarta(datosJugador[j].getEnBazas().cartaEn(k))=='O'){
                        joros++;
                    }
                    if (BarajaEsp.numeroDeCarta(datosJugador[j].getEnBazas().cartaEn(k))=='7'){
                        jsietes++;
                    }
                }
                if (ioros<joros){
                    flag_oros=false;
                }
                if (isietes<jsietes){
                    flag_sietes=false;
                }
            }
        }
        
        if (flag_cartas){
            puntos++;
        }
        if (flag_oros){
            puntos++;
        }
        if (flag_sietes){
            puntos++;
        }
        puntos+=datosJugador[i].getEscobas();
        return puntos;
    }
    
    /**
     * Devuelve una representacion algo mas amigable que la encontrada en 
     * toString. Se usara para presentar el juego actual cuando se juega en
     * modo texto. No hace falta que contenga toda la informacion, basta con
     * que sea facil de leer. El formato es bastante libre.
     * @param i indice del jugador para el cual se genera la representacion.
     * @return la cadena pedida.
     */
    public String getRepresentacion(int i) {
        System.out.println("Cartas en la mesa:");
        for (int j=0; j<cartasEnMesa.size();j++){
            System.out.println(cartasEnMesa.cartaEn(j).getNombre());
        }
        System.out.println("");
        System.out.println("Cartas en tu mano:");
        for (int j=0; j<datosJugador[i].getEnMano().size();j++){
            System.out.println(datosJugador[i].getEnMano().cartaEn(j).getNombre());
        }
        System.out.println("");
        System.out.println("Bazas: " + datosJugador[i].getEnBazas().toString());
        System.out.println("Escobas: " + datosJugador[i].getEscobas());
        System.out.println("Vueltas para finalizar la ronda: " + cartasPorRepartir.size()/3/numJugadores);
        System.out.println("etcetera");
        return "";
    }
}
