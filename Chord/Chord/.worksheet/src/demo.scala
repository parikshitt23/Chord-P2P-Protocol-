
import scala.util.Random
import java.security.MessageDigest
import scala.collection.mutable.ArrayBuffer
import collection.mutable.ArrayLike
import collection.immutable.Map
import java.math.BigInteger
import scala.math
import com.sun.org.apache.xalan.internal.xsltc.compiler.Template

object demo {;import org.scalaide.worksheet.runtime.library.WorksheetSupport._; def main(args: Array[String])=$execute{;$skip(374); 

   
var tempList: List[Int] = 5::7:: 30:: 31:: 39:: 60:: 63::80:: 86:: Nil;System.out.println("""tempList  : List[Int] = """ + $show(tempList ));$skip(22); 
var x = tempList.head;System.out.println("""x  : Int = """ + $show(x ));$skip(25); 
tempList = tempList.tail;$skip(26); 

println(tempList.toList)}

}
