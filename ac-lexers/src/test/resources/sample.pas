program BottlesOfBeer;
 
var
    i: integer;
 
begin
    for i := 99 downto 1 do
        if i <> 1 then
            begin
                writeln(i, ' bottles of beer on the wall');
                writeln(i, ' bottles of beer');
                writeln('Take one down, pass it around');
                if i = 2 then
                    writeln('One bottle of beer on the wall')
                else
                    writeln(i - 1, ' bottles of beer on the wall');
                writeln;
            end
        else
            begin
                writeln('One bottle of beer on the wall');
                writeln('One bottle of beer');
                writeln('Take one down, pass it around');
                writeln('No more bottles of beer on the wall');
            end
end.
