#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "huffman.h"
#include "utilAA.h"
#include "pq.h"
#include "math.h"

#define MIN_ALLOC 8

int MASK[] = {1, 2, 4, 8, 16, 32, 64, 128};
int IMASK[] = {254, 253, 251, 247, 239, 223, 191, 127};



 /*****************************************************************************
 FUNCION: histograma

 Descripcion: Genera un histograma de frecuencias a partir de un array de bytes

 Entrada:
     in: array de bytes
     n_bytes: número de bytes
     bits_letra: número de bits que tiene una letra

 Salida:
     histograma: array con el número de veces que aparece cada letra en 'in'.
     NULL:  en caso de error.
*****************************************************************************/
int *histograma(byte *in, int n_bytes, int bits_letra)
{
    int *histograma=NULL;
    int i=0,long_histograma=0;


    /* longitud del histograma, 2 elevado al numero de bits de que tiene la letra*/
    long_histograma=1<<bits_letra;


    /* se reserva memoria para el array 'histograma'*/
    histograma=(int*)callocAA(long_histograma,sizeof(int));
    if(histograma==NULL)
    {
        printf("ERROR:histograma.Error en la reserva de memoria\n");
        return NULL;

    }

    /* se inicializa el array a 0*/
    memset(histograma,0,long_histograma);

    /* se cuentan las letras dependiendo del numero de bits que tengan*/
    switch(bits_letra)
    {

        /* la letra tiene 4 bits*/
        case 4:
                   for(i=0;i<n_bytes;i++)
                   {
		       /* se saca la primera letra del byte (empezando por la izquierda), poniendo a ceros la parte baja
		          del byte y desplazando 4 bits a la derecha y se incrementa su contador*/
		       histograma[(in[i] & 0xf0) >> 4] ++;

                      /* se saca la segunda letra del byte, poniendo a ceros la parte alta del byte */
		       histograma[in[i] & 0x0f] ++;

                   }
		   break;


	/* la letra tiene 8 bits*/
	case 8:   for(i=0;i<n_bytes;i++) histograma[in[i]] ++; break;

	/* la letra tiene 16 bits*/
        case 16:

	          for(i=0;i<n_bytes;i=i+2)
                  {
		       /* si n_bytes es par se desplazan 8 bits a la izquierda y se une con el siguiente byte*/
                      if (n_bytes%2==0)  histograma[(in[i] << 8) | in[i+1]]++;

                      else /* si n_bytes es impar, el último byte se une a ceros */
                      {
	                   if (n_bytes-1==i)    histograma[(in[i] << 8) | 0x00]++;
                           else   histograma[(in[i] << 8) | in[i+1]]++;
                      }
                  }

                  break;
        default: return NULL;
     }

    return histograma;
}




/*****************************************************************************
 FUNCION: crea_tabla_codigos

 Descripcion: Obtiene los codigos Huffman para una cadena dada

 Entrada:
     in: array de bytes
     n_bytes: número de bytes
     bits_letra: número de bits que tiene una letra

 Salida:
     tabla_huffman:  la tabla conversión de cada código original
     NULL:  en caso de error.
*****************************************************************************/
tabla_huffman *crea_tabla_codigos(byte *in, int n_bytes, int bits_letra)
{
    tabla_huffman *th=NULL;
    int *h = NULL;
    PCOLAP colap=NULL;
    ELEMENTOP e1, e2;
    nodo_huffman *n=NULL, *n1=NULL, *n2=NULL, *nfusion=NULL;
    int i=0;

    /* se crea el histograma*/
    h=histograma(in,n_bytes,bits_letra);
    if (h==NULL)
    {
         printf("ERROR:crea_tabla_codigos.Error al crear el histograma\n");
         return NULL;
    }

    /* se crea la cola de prioridad */
    colap=PQIni(1<<bits_letra);


    /* se reserva memoria para la tabla huffman */
    th=(tabla_huffman*)callocAA(1,sizeof(tabla_huffman));
    if (th==NULL)
    {
         printf("ERROR:crea_tabla_codigos.Error en la reserva de memoria\n");
         return NULL;
    }

    /* se reserva memoria para el array de 'codigos' de la tabla huffman */
    th->codigos=(bit_code*)callocAA((1<<bits_letra),sizeof(bit_code));
    if (th->codigos==NULL)
    {
         printf("ERROR:crea_tabla_codigos.Error en la reserva de memoria\n");
         return NULL;
    }

    /* se inicializan los campos de la estructura 'tabla_huffman'*/
    th->n_codigos=0;
    th->bits_letra=bits_letra;


    /* se reserva memoria para cada 'codigo' de cada letra */
    for(i=0;i<(1<<bits_letra);i++)
    {

        th->codigos[i].codigo=(byte*)callocAA(1,sizeof(byte));
	if (th->codigos==NULL)
        {
             printf("ERROR:crea_tabla_codigos.Error en la reserva de memoria\n");
             return NULL;
        }
        th->codigos[i].n_bits=0;
    }



    /* se introducen en la cola de prioridad los nodos de las letras originales */
    for (i=0;i<(1<<bits_letra);i++)
    {
       /* si existe la letra en el histograma se mete en un nodo */
       if (h[i]!=0)
       {
           /* se reserva memoria para el nodo y para el array de letras que lo componen, en este momento una */
           if ((n=(nodo_huffman*) callocAA(1,sizeof(nodo_huffman)))==NULL)  return NULL;
           if ((n->letras=(unsigned int*) callocAA(1,sizeof(unsigned int)))==NULL) return NULL;

           n->letras[0]=i;
           n->n_letras=1;
           n->peso=h[i];
	   /* se añade el nodo en la cola de prioridad según el peso*/
           PQAdd(n,n->peso,colap);
       }
    }



    /* se sacan los nodos de la cola de dos en dos y se funden en un solo nodo que se vuelve a insertar */
    while(1)
    {
         PQRem(&e1,colap);
         n1=(nodo_huffman*) e1.datos;

	 /* si la cola no esta vacía se saca el segundo elemento */
         if (!PQVacio(colap))
         {
             PQRem(&e2,colap);
             n2=(nodo_huffman *) e2.datos;
         }
         else break;


	/* se funden ambos nodos en un solo combinación de los dos */
	if ((nfusion=fundir_nodos(n1,n2,th))==NULL)  return NULL;


	/* se añade el nodo fundido en la cola de prioridad */
	PQAdd(nfusion,nfusion->peso,colap);

        /* se liberan los nodos utilizados */
	freeAA(n1->letras);
        freeAA(n1);
        freeAA(n2->letras);
        freeAA(n2);
    }

    /* se libera la memoria utilizada para la creación de la tabla huffman y que ya no se necesita */
    freeAA(h);
    PQLiberar(colap);
    freeAA(((nodo_huffman*)e1.datos)->letras);
    freeAA(e1.datos);

    return th;
}



/*****************************************************************************
 FUNCION: fundir_nodos

 Descripcion: Realiza la fusión de dos nodos y rellena la tabla huffman

 Entrada:
     n1: nodo 1
     n2: nodo 2
     th: tabla huffman

 Salida:
     nodo_huffman:  un nodo fusión de los dos pasados como parámetros.
     NULL:  en caso de error.
*****************************************************************************/
nodo_huffman* fundir_nodos(nodo_huffman*n1,nodo_huffman*n2,tabla_huffman*th)
{
    nodo_huffman *nfusion=NULL;
    int i=0, j=0;

    /* se reserva memoria para el nodo fusionado*/
    nfusion=(nodo_huffman*)callocAA(1,sizeof(nodo_huffman));
    if (nfusion==NULL)
    {
        printf("ERROR:fundir_nodos.Error en la reserva de memoria\n");
        return NULL;
    }


    /* el número de letras del nodo fusionado será la suma de las letras de cada nodo */
    nfusion->n_letras = n1->n_letras + n2->n_letras;


    /* el peso del nodo fusion es la suma de los pesos de los otros dos nodos*/
    nfusion->peso = n1->peso + n2->peso;


    /* se reserva memoria para las letras que formarán el nodo fusionado */
    nfusion->letras = (unsigned int*)callocAA(nfusion->n_letras,sizeof(unsigned int));
    if (nfusion->letras==NULL)
    {
        printf("ERROR:fundir_nodos.Error en la reserva de memoria\n");
        return NULL;
    }


    /* se copia cada letra del nodo 1 en el nodo fusión */
    for (i=0;i<n1->n_letras;i++)
         nfusion->letras[i] = n1->letras[i];


    /* se copian las letras del nodo 2 detrás de las letras del nodo 1 en el array 'letras' del nodo fusión */
    for (i=n1->n_letras;i<nfusion->n_letras;i++)
    {
         nfusion->letras[i] = n2->letras[j];
         j++;
    }


                                           /* se rellena la tabla huffman */
    /* se analiza el primer nodo */
    for(i=0;i<n1->n_letras;i++)
    {
         /* si el numero de bits es mayor que 0 y múltiplo de 8 se realoca la memoria */
         if ((th->codigos[n1->letras[i]].n_bits>0)&&(th->codigos[n1->letras[i]].n_bits%8==0))         th->codigos[n1->letras[i]].codigo=(byte*)reallocAA(th->codigos[n1->letras[i]].codigo,((th->codigos[n1->letras[i]].n_bits/8)+1)*sizeof(byte));

         /* se pone a uno el bit del nodo 1 que correponda */
         SET(th->codigos[n1->letras[i]].n_bits,th->codigos[n1->letras[i]].codigo);
         (th->codigos[n1->letras[i]].n_bits)++;/* se incrementa el bit del byte del codigo que se está utilizando */
   }



   /* se hace lo mismo con el otro nodo pero poniéndole código 0 en el bit que corresponda */
   for(i=0;i<n2->n_letras;i++)
   {
        /* si el numero de bits es multiplo de 8 y es mayor que 0, se realoca memoria */
        if ((th->codigos[n2->letras[i]].n_bits>0)&&(th->codigos[n2->letras[i]].n_bits%8==0))	th->codigos[n2->letras[i]].codigo=(byte*)reallocAA(th->codigos[n2->letras[i]].codigo,((th->codigos[n2->letras[i]].n_bits / 8)+1)*sizeof(byte));

        /* se pone a 0 el bit del nodo 2 que corresponda */
        CLEAR(th->codigos[n2->letras[i]].n_bits,th->codigos[n2->letras[i]].codigo);
        (th->codigos[n2->letras[i]].n_bits)++;
   }



   return nfusion;
}




/*****************************************************************************
 FUNCION: libera_tabla_codigos

 Descripcion: Libera una tabla de codigos

 Entrada:
     th: tabla huffman

 Salida:
     nada
*****************************************************************************/
void libera_tabla_codigos(tabla_huffman **th)
{
    int i,tam_tabla;

    tam_tabla=(1<<(*th)->bits_letra);

    /* se libera la tabla de codigos de la tabla huffman */
    for(i=0;i<tam_tabla;i++) freeAA((*th)->codigos[i].codigo);
    freeAA((*th)->codigos);


    /* se libera la tabla huffman*/
    freeAA(*th);
}


/*****************************************************************************
 FUNCION: comprime

 Descripcion: Comprime un array de bytes usando los codigos contenidos en tabla_huffman

 Entrada:
     in: array de bytes
     n_bytes: número de bytes
     th: tabla huffman
     n_bits: número de bits que tendrá la cadena comprimida

 Salida:
     nada
*****************************************************************************/
byte *comprime(byte *in, int n_bytes, tabla_huffman *th, int *n_bits)
{
    byte *resultado = NULL;
    int k=0,i=0,letra=0,j=0;


    /* se reserva memoria para el resultado de la compresión */
    resultado = (byte *) callocAA(n_bytes, sizeof(byte));
    if (resultado==NULL)
    {
        printf("ERROR:comprime.Error en la reserva de memoria\n");
        return NULL;
    }


    /* se recorre el array de entrada y se saca la codificación de cada letra de la tabla huffman*/
    for(i=0;i<n_bytes*8;i=i+th->bits_letra)
    {

        letra = lee_letra(in,i,th->bits_letra,n_bytes);

	/* se recorren los bits de la codificación de cada letra y se insertan en el array 'resultado' */
        for(j=0;j<th->codigos[letra].n_bits;j++)
        {
            if(GET(j, th->codigos[letra].codigo))
	       SET(k,resultado);
            else
	       CLEAR(k,resultado);
            k++;
        }
    }

    *n_bits=k;
    return resultado;

}



/*****************************************************************************
 FUNCION: lee_letra

 Descripcion: Lee la letra correspondiente a un índice pasado como parámetro
              del array de entrada 'in'.

 Entrada:
     in: array de bytes
     i: índice de donde se sacará la letra.
     bits_letra: número de bits de la letra
     n_bytes: número de bytes del array 'in'

 Salida:
     nada
*****************************************************************************/
int lee_letra(byte*in,int i,int bits_letra,int n_bytes)
{

     /* se devuelve la letra según el número de bits que tenga */
     switch(bits_letra)
     {
        /* letra con 4 bits */
        case 4:
                   if (i%8==0) return ((in[i/8] & 0xf0)>>4);
                   else if (i%8==4) return (in[(i/8)] & 0x0f);break;


	/* letra con 8 bits */
        case 8:  return in[i/8];break;


	/* letra con 16 bits */
        case 16:


		 if (n_bytes%2==0) return ((in[i/8] << 8) | in[(i/8)+1]);
		 else
		 {
		     if (n_bytes-1==(i/8)) return ((in[i/8] << 8) | 0x00);
                     else return ((in[i/8] << 8) | in[(i/8)+1]);
		 }
                 break;

        default: return -1; break;
     }
return 0;
}




/*
 * Descomprime un array de bytes comprimido con la tabla_huffman proporcionada
 * puede devolver un numero de bytes mayor que la cadena original, si la ultima
 * palabra de la cadena original no cabia del todo; esto se debe corregir fuera
 */
byte *descomprime(byte *in,  int n_bits, tabla_huffman *th, int *n_bytes) {
    /* (PRACTICA: a rellenar) */
    return NULL;
}






/*** funciones auxiliares ***/

/*
 * Muestra un histograma; omite los simbolos con 0 ocurrencias, usa 4 columnas
 */
void muestra_histograma(int *ocurrencias, int n_letras) {
    int i, j;
    for (i=0, j=0; i<n_letras; i++) {
        if (ocurrencias[i] == 0) continue;
        printf("%6d --> %6d%s", i, ocurrencias[i], ((j++ %4) == 3)?"\n":"    ");
    }
    if ((j%4) != 0) printf("\n");
}

/*
 * calcula la entropia de un histograma, usando la formula
 * H = - SUM(p[i] * log2(p[i]) con p[i] = probabilidad de la letra 'i'
 */
double entropia(int *ocurrencias, int n_letras) {
    int i, t;
    double p, l2p, suma;
    double ln2 = log(2.0);
    for (i=0, t=0; i<n_letras; i++) t += ocurrencias[i];
    suma = 0;
    for (i=0; i<n_letras; i++) {
        if (ocurrencias[i] == 0) continue;
        p = ocurrencias[i]/(double)t;
        l2p = log(p)/ln2;
        suma += p*l2p;
    }
    return -suma;
}

/* 
 * muestra una secuencia de bits sacada de un buffer;
 * La secuencia 0x0F,0x1F se muestra como 0000 1111 0001 1111
 * (bytes en orden, en cada byte, bit mas significativo primero)
 */
void muestra_bits(byte *donde, int bit_ini, int bit_fin) {
    int i;
    if (bit_fin - bit_ini == 0) printf ("|");
    for (i=bit_ini; i<bit_fin; i++) {
        printf((donde[i>>3] & MASK[7-(i&7)])?"1":"0");
    }
    return;
}

/* 
 * Estima el tamanyo de la cabecera necesaria para poder reconstruir 
 * un fichero comprimido con la tabla proporcionada. Usa el mas eficiente de
 * dos formatos posibles:
 *
 * formato #1: (mejor si hay muchas letras de entre las posibles)
 *  1 byte para numero de bits validos en el ultimo byte
 *  1 byte para numero de bytes validos en la ultima palabra al descomprimir
 *  1 byte para bits/letra
 *  - para cada letra posible (2^bits_letra posibles):
 *    1 byte para numero de bits del codigo de la letra i-esima usados; 
 *      (si > 254 bits son necesarios, se usan el 'byte bandera' 255 y otros 2 bytes)
 *  - para cada letra posible, y en el mismo orden anterior, y usando solo
 *    los bits precisos, el codigo en si (Â¡si no aparece, son 0 bits!)
 * el resultado se redondea al siguiente byte.
 *
 * formato #2: (mejor si muchas letras no aparecen)
 *  1 byte igual que antes, pero en negativo (indica cabecera tipo #2)
 *  2 bytes igual que antes
 *  2 bytes para numero de letras que aparecen de verdad
 *  - para cada letra que aparecen
 *    2 bytes con la letra (puede que sobre espacio)
 *    1 Ã³ 3 bytes con el numero de bits de su codigo (ver formato #1)
 *  - para cada letra que aparece
 *    los bits precisos
 * el resultado tambien se redondea
 */
int estima_cabecera(tabla_huffman* th) {
    int i;
    int n_letras = 1<<th->bits_letra;
    int bits_codigo = 0;
    int total1 = 3;
    int total2 = 5;
    for (i=0; i<n_letras; i++) {    
        if (th->codigos[i].n_bits < 255) {
            total1 ++;
            if (th->codigos[i].n_bits > 0) {
                total2 += 3;
            }
        }
        else {
            total1 += 3; /* un byte de escape, y 2 para el numero de bits */
            total2 += 5; /* igual, pero ademas 2 para la letra */
        }
        bits_codigo += th->codigos[i].n_bits;
    }

    /* suma los bits de los codigos al total */
    total1 += (bits_codigo + (bits_codigo&7))>>3;
    total2 += (bits_codigo + (bits_codigo&7))>>3;

    return total1 < total2 ? total1 : total2;
}

/*** (PRACTICA: puede ser util usar algunas funciones auxiliares mas) ***/
