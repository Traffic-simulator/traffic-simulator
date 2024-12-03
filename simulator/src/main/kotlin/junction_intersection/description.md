## straight line
```
Example:
<planView>
    <geometry
        s="0.0000000000000000e+00"
        x="-4.7170752711170401e+01"
        y="7.2847983820912710e-01"
        hdg="6.5477882613167993e-01"
        length="5.7280000000000000e+01">
        <line/>
    </geometry>
</planView>
```
x,y - стартовые точки
hdg - угол наклона
length - длинна прямой
s - координата стартовой позиции (как я понял от куда начинать рисовать линию) 
x(s) = x + s * cos(hdg)  
y(s) = y + s * sin(hdg)

## spiral

```
<geometry s="100.0" x="38.00" y="-1.81" hdg="0.33" length="30.00">
    <spiral curvStart="0.0" curvEnd="0.013"/>
</geometry>
```


