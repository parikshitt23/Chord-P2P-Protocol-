
import scala.util.Random
import java.security.MessageDigest
import scala.collection.mutable.ArrayBuffer
import collection.mutable.ArrayLike
import collection.immutable.Map
import java.math.BigInteger
import scala.math
import com.sun.org.apache.xalan.internal.xsltc.compiler.Template

object demo {

   
var tempList: List[Int] = 5::7:: 30:: 31:: 39:: 60:: 63::80:: 86:: Nil
                                                  //> tempList  : List[Int] = List(5, 7, 30, 31, 39, 60, 63, 80, 86)
var x = tempList.head                             //> x  : Int = 5
tempList = tempList.tail

println(tempList.toList)                          //> List(7, 30, 31, 39, 60, 63, 80, 86)

}