package readablelong

fun Long.readable(): String{
    val str = toString()
    val newString = CharArray(str.length + (str.length / 3 - if(str.length % 3 == 0) 1 else 0))
    var i = 0
    (0 until str.length % 3).forEach{
        newString[it] = str[it]
        i++
    }
    var source = i
    if(i != 0 && newString.size != i){
        newString[i] = ','
        i++
    }
    var writing = 0
    (i until newString.size).forEach{
        newString[it] = if(writing == 3) ',' else str[source]
        if(writing == 3){
            writing = 0
        }else{
            source++
            writing++
        }
    }
    return newString.concatToString()
}