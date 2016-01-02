import scala.math._
import scala.util.Random
import akka.actor.Actor
import akka.actor.Props
import akka.actor._
import akka.actor.ActorRef
import akka.actor.ActorSystem
import scala.concurrent.duration.Duration
import scala.concurrent.duration.FiniteDuration
import java.util.concurrent.TimeUnit
import akka.util
import collection.mutable.HashMap
import java.math.BigInteger
import collection.mutable.Cloneable
import com.sun.org.apache.bcel.internal.generic.INSTANCEOF


case class InitMaster(nodeIDList: List[Int], numRequests: Int, m: Int, joiningNode: List[Int])
case class initNode(nodeID: Int, successor: Int, predecssor: Int, fingerTable: Array[String], m: Int, numRequests: Int, allKeys: List[Int], hopActor: ActorRef)
case class lookupFingerTable(key: Int, requestFrom: Int, hopCount: Int)
case class startRequest()
case class informNodes(networkNodes: Array[ActorRef])
case class requestCompleted(totalHops: Int)
case class calculateHop(hopCount: Int)
case class initiateJoin()
case class join(joiningNodeId: Int, knownNode: ActorRef, networkNodes: Array[ActorRef], numRequests: Int, hopActor: ActorRef)
case class getObj()
case class setObj(receivedObj: nodeActor, nodeId: Int)
case class initJoining()
case class getKnownObj()
case class setKnownObj(receivedObj: nodeActor)
case class updateFinger(affectedNodes: List[Int], updatedValue: Int)
case class startQuerying()
case class updateKeys()
case class setNewKeys(newKeys: List[Int])
case class initWatcher(masterNode:ActorRef, joinCount:Int)
case class afterJoin(currentNodes:List[Int])
case class joinWatcher(currentNodes:List[Int], currentJoinedCount:Int)
case class sendNextNode(nodeIndex:Int)

class averageHopCalculator(numNodes: Int, numRequests: Int) extends Actor {
  var totalHops: Double = 0
  var totalRequests: Double = numNodes * numRequests
  var receivedReq: Double = 0
  var masterNode:ActorRef = null
  var joinCount:Int = 0
  var currentNodes:List[Int]= List() 
  var currentJoinedCount:Int = 0
  def receive = {
    case calculateHop(hCount: Int) => {
      totalHops += hCount
      receivedReq += 1
      
      
      if (receivedReq == totalRequests) {
        
        var avgHops: Double = totalHops / totalRequests
        println("All Requests Completed...")
        println("Total Hops:" + totalHops)
        println()
        println("Average Number Hops: " + avgHops)

      }

    }
    
    case initWatcher(mNode:ActorRef, jCount:Int)=>{
      masterNode = mNode
      joinCount = jCount
      
    }
    
    case joinWatcher(cNodes:List[Int], currentJCount:Int)=>{
      currentNodes = cNodes
      currentJoinedCount +=1
      
      if(currentJoinedCount == joinCount ){
        sender ! afterJoin(currentNodes)
        
      }else{
        masterNode ! sendNextNode(currentJoinedCount)
      }
      
      
    }

  }

}
class nodeActor(mVal: Int) extends Actor {
  var nodeId: Int = 0
  var successor: Int = 0
  var predecessor: Int = 0
  var fingerTable = Array.ofDim[String](mVal)
  var m: Int = 0
  var key: Int = 0
  var allKeys: List[Int] = List()
  var requestFrom: Int = 0
  var hopCount: Int = 0
  var requestTicker: Cancellable = null
  var numRequests: Int = 0
  var requestCount: Int = 0
  var requestCompleteCount: Int = 0
  var fingerTableStart = Array.ofDim[Int](mVal)
  var fingerTableNode = Array.ofDim[Int](mVal)
  var networkNodes: Array[ActorRef] = null
  var hopActor: ActorRef = null
  var knownNode: ActorRef = null
  var nodeSpace: Int = math.pow(2, mVal).toInt
  var knownNodeObj: nodeActor = null
  var nodesObj: Array[nodeActor] = Array.ofDim[nodeActor](nodeSpace)

  import context.dispatcher
  def receive = {
    case initNode(nid: Int, succ: Int, pred: Int, lookup: Array[String], mValue: Int, requestNumber: Int, totalKeys: List[Int], hActor: ActorRef) => {
      nodeId = nid
      successor = succ
      predecessor = pred
      fingerTable = lookup
      m = mValue
      numRequests = requestNumber
      allKeys = totalKeys
      hopActor = hActor
      for (i <- 0 to m - 1) {
        fingerTableStart(i) = fingerTable(i).split(",")(0).toInt
        fingerTableNode(i) = fingerTable(i).split(",")(1).toInt
      }
      

    }

    case informNodes(allNodes: Array[ActorRef]) => {
      networkNodes = allNodes
      var x = 0
      if (networkNodes.length > 1000) {
        x = 1000
      }
      
    }

    case join(joiningId: Int, kNode: ActorRef, allNodes: Array[ActorRef], requestNumber: Int, hActor: ActorRef) => {
      nodeId = joiningId
      knownNode = kNode
      m = mVal
      networkNodes = allNodes
      numRequests = requestNumber
      hopActor = hActor
      knownNode ! getKnownObj()
      for (i <- 0 to m - 1) {
        var start = (nodeId + math.pow(2, i).toInt) % math.pow(2, m).toInt
        fingerTable(i) = (start + ",X")
      }
      
      for (i <- 0 to networkNodes.length - 1) {
        if (networkNodes(i) != null) {
          networkNodes(i) ! getObj()
        }

      }
      context.system.scheduler.scheduleOnce(FiniteDuration(3000, TimeUnit.MILLISECONDS), self, initJoining())

    }

    case initJoining() => {
      initFingerTable(knownNodeObj)
      updateOthersFingerTable()

      context.system.scheduler.scheduleOnce(FiniteDuration(3000, TimeUnit.MILLISECONDS), self, updateKeys())

    }
    case updateKeys() => {
     
      var presentNodes: List[Int] = List()
      for (i <- 0 to networkNodes.length - 1) {
        if (networkNodes(i) != null) {
          presentNodes ::= nodesObj(i).nodeId
        }
        
      }
      presentNodes = presentNodes.sorted
      var newKeys: List[Int] = List()
      for (index <- 0 to presentNodes.length - 1) {
        if (index == 0) {
          var x = presentNodes(presentNodes.length - 1) + 1
          newKeys = List()
          for (i <- x to math.pow(2, m).toInt - 1) {
            newKeys ::= i
          }
          for (i <- 0 to presentNodes(index)) {
            newKeys ::= i
          }
        } else if (index != 0 && index <= presentNodes.length - 1) {
          newKeys = List()
          for (i <- presentNodes(index - 1) + 1 to presentNodes(index)) {
            newKeys ::= i
          }
        }
        //println(newKeys.toList)
        networkNodes(presentNodes(index)) ! setNewKeys(newKeys)
      }
      println("Node "+nodeId+" Joined..")
      
      
      hopActor ! joinWatcher(presentNodes, 1)
      
      
    }
    
    case afterJoin(currentNodes:List[Int])=>{
      println()
      println("Request Processing Started...")
      for (i <- 0 to currentNodes.length - 1) {
        networkNodes(currentNodes(i)) ! startQuerying()
      }
      
    }

    case startQuerying() => {
      requestTicker = context.system.scheduler.schedule(FiniteDuration(5000, TimeUnit.MILLISECONDS), FiniteDuration(1000, TimeUnit.MILLISECONDS), self, startRequest())

    }

    case getObj() => {
      sender ! setObj(this, nodeId)

    }
    case setObj(rObj: nodeActor, id: Int) => {
      nodesObj(id) = rObj
    }
    case getKnownObj() => {
      sender ! setKnownObj(this)

    }
    case setKnownObj(rObj: nodeActor) => {
      knownNodeObj = rObj
    }

    case setNewKeys(nKeys: List[Int]) => {
      allKeys = nKeys
    }

    case updateFinger(aNodes: List[Int], newValue: Int) => {

      for (i <- 0 to m - 1) {
        fingerTableStart(i) = fingerTable(i).split(",")(0).toInt
        fingerTableNode(i) = fingerTable(i).split(",")(1).toInt
      }
      for (i <- 0 to aNodes.length - 1) {
        if (fingerTableStart.contains(aNodes(i))) {
          //println(fingerTableStart.indexOf(aNodes(i)))
          fingerTable(fingerTableStart.indexOf(aNodes(i))) = fingerTable(fingerTableStart.indexOf(aNodes(i))).split(",")(0) + "," + newValue

        }

      }
      
      
      for (i <- 0 to m - 1) {
        fingerTableStart(i) = fingerTable(i).split(",")(0).toInt
        fingerTableNode(i) = fingerTable(i).split(",")(1).toInt
      }
      
    }

    case lookupFingerTable(keyValue: Int, orgNode: Int, hop: Int) => {
      var key = keyValue
      requestFrom = orgNode
      hopCount = hop + 1

      if (allKeys.contains(key)) {
        //println("I am"+nodeId+"found"+key+"requestFrom" + requestFrom)
        networkNodes(orgNode) ! requestCompleted(hopCount)
      } else if (fingerTableStart.contains(key)) {
        //println("I am" + nodeId + "requestFrom" + requestFrom + "key" + key + "forwarding to" + fingerTableNode(fingerTableStart.indexOf(key)))
        networkNodes(fingerTableNode(fingerTableStart.indexOf(key))) ! lookupFingerTable(key, requestFrom, hopCount)
      } else {
        if (cyclicCheck(key, fingerTableStart(m - 1), fingerTableStart(0))) {
          //println("I am" + nodeId + "requestFrom" + requestFrom + "key" + key + "forwarding to" + fingerTableNode(m - 1))
          networkNodes(fingerTableNode(m - 1)) ! lookupFingerTable(key, requestFrom, hopCount)
        } else {
          for (i <- 0 to m - 2) {
            if (cyclicCheck(key, fingerTableStart(i), fingerTableStart(i + 1))) {
              //println("III am" + nodeId + "requestFrom" + requestFrom + "key" + key + "forwarding to" + fingerTableNode(i))
              networkNodes(fingerTableNode(i)) ! lookupFingerTable(key, requestFrom, hopCount)
            }

          }

        }

      }

    }

    case startRequest() => {
      var newKey = scala.util.Random.nextInt(math.pow(2, m).toInt)
      requestCount += 1
      if (requestCount <= numRequests) {
        self ! lookupFingerTable(newKey, nodeId, -1)
      } else {
        requestTicker.cancel()
      }

    }

    case requestCompleted(hopCount: Int) => {
      requestCompleteCount += 1
      hopActor ! calculateHop(hopCount)
      if (requestCompleteCount <= numRequests) {
      }

    }

  }
  def cyclicCheck(Id: Int, first: Int, second: Int): Boolean = {
    if (first < second) {
      if (Id > first && Id < second) { return true }
      else return false
    } else {
      if (Id > first || Id < second) { return true }
      else return false
    }
  }

  def initFingerTable(kNodeObj: nodeActor) = {
    
    this.successor = findSuccessor(kNodeObj.nodeId, fingerTable(0).split(",")(0).toInt)
    fingerTable(0) = fingerTable(0).split(",")(0) + "," + findSuccessor(kNodeObj.nodeId, fingerTable(0).split(",")(0).toInt)
    this.predecessor = nodesObj(this.successor).predecessor
    nodesObj(this.successor).predecessor = this.nodeId 
    for (i <- 0 to m - 2) {
      if (cyclicCheck2(fingerTable(i + 1).split(",")(0).toInt, nodeId, fingerTable(i).split(",")(1).toInt)) {
        fingerTable(i + 1) = fingerTable(i + 1).split(",")(0) + "," + fingerTable(i).split(",")(1)
      } else {

        fingerTable(i + 1) = fingerTable(i + 1).split(",")(0) + "," + findSuccessor(kNodeObj.nodeId, fingerTable(i + 1).split(",")(0).toInt)
      }

    }
    nodesObj(this.predecessor).successor = this.nodeId
  }

  def updateOthersFingerTable() = {
    var affectedNodes: List[Int] = null
    if (this.nodeId < this.predecessor) {
      affectedNodes = List()
      for (i <- this.predecessor + 1 to math.pow(2, m).toInt - 1) {
        affectedNodes ::= i
      }
      for (i <- 0 to this.nodeId) {
        affectedNodes ::= i
      }
    } else {
      affectedNodes = List()
      for (i <- this.predecessor + 1 to this.nodeId) {
        affectedNodes ::= i
      }
    }
    
    for (i <- 0 to networkNodes.length - 1) {
      if (networkNodes(i) != null) {
        networkNodes(i) ! updateFinger(affectedNodes, nodeId)
      }
    
    }

  }

  def findSuccessor(kNodeId: Int, newNodeId: Int): Int = {
    
    var tempNode = nodesObj(findPredecessor(kNodeId, newNodeId)).successor 
    return tempNode
  }

  def findPredecessor(kNodeId: Int, newNodeId: Int): Int = {

    var tempNodenodeId = nodesObj(kNodeId).nodeId
    var tempNodesuccessor = nodesObj(kNodeId).fingerTable(0).split(",")(1).toInt

    while (!cyclicCheck3(newNodeId, tempNodenodeId, tempNodesuccessor)) {
      tempNodenodeId = closestPrecedingFinger(tempNodenodeId, newNodeId)
      tempNodesuccessor = nodesObj(tempNodenodeId).fingerTable(0).split(",")(1).toInt
    }
    return tempNodenodeId
  }

  def closestPrecedingFinger(kNodeId: Int, newNodeId: Int): Int = {
    for (i <- m - 1 to 0 by -1) {
      var s = nodesObj(kNodeId).fingerTable(i).split(",")
      if (cyclicCheck1(s(1).toInt, kNodeId, newNodeId)) {
        return s(1).toInt
      }
    }
    return kNodeId

  }

  def cyclicCheck1(Id: Int, nid: Int, succId: Int): Boolean = {
    if (nid < succId) {
      if (Id > nid && Id < succId) { return true }
      else return false
    } else {
      if (Id > nid || Id < succId) { return true }
      else return false
    }
  }

  def cyclicCheck2(Id: Int, nid: Int, succId: Int): Boolean = {
    if (nid < succId) {
      if (Id >= nid && Id < succId) { return true }
      else return false
    } else {
      if (Id >= nid || Id < succId) { return true }
      else return false
    }
  }

  def cyclicCheck3(Id: Int, nid: Int, succId: Int): Boolean = {
    if (nid < succId) {
      if (Id > nid && Id <= succId) { return true }
      else return false
    } else {
      if (Id > nid || Id <= succId) { return true }
      else return false
    }
  }

}
class Master(mVal: Int) extends Actor {
  var nodeIDList: List[Int] = null
  var nodes: Array[ActorRef] = null
  var m: Int = 0
  var numRequests: Int = 0
  var key: Int = 0
  var joiningNode: List[Int] = List()
  var knownNode: ActorRef = null
  var peerNodes = Array.ofDim[ActorRef](math.pow(2, mVal).toInt)
  var hopActor:ActorRef = null
  def receive = {
    case InitMaster(idList: List[Int], requestNumber: Int, mValue: Int, nextNode: List[Int]) => {
      nodeIDList = idList.sorted
      m = mValue
      numRequests = requestNumber
      nodes = Array.ofDim[ActorRef](math.pow(2, m).toInt)
      joiningNode = nextNode
      var predecessor: Int = 0
      var successor: Int = 0
      hopActor = context.system.actorOf(Props(new averageHopCalculator((nodeIDList.length + joiningNode.length), numRequests)))
      hopActor ! initWatcher(self, joiningNode.length)
      for (index <- 0 to nodeIDList.length - 1) {
        var fingerTable = Array.ofDim[String](m)
        var keys: List[Int] = List()
        peerNodes(nodeIDList(index)) = context.system.actorOf(Props(new nodeActor(m)))
        //set Successor and predecessor for initial Network
        if (index == 0) {
          predecessor = nodeIDList(nodeIDList.length - 1)
          successor = nodeIDList(index + 1)
        } else if (index == nodeIDList.length - 1) {
          predecessor = nodeIDList(index - 1)
          successor = nodeIDList(0)
        } else {
          predecessor = nodeIDList(index - 1)
          successor = nodeIDList(index + 1)
        }

        //set FingerTable for initial Network
        for (fingerindex <- 0 to m - 1) {

          var start = (nodeIDList(index) + math.pow(2, fingerindex).toInt) % math.pow(2, m).toInt
          var fingerNode: Int = 0
          if (nodeIDList.contains(start)) {
            fingerNode = start
          } else if (start > nodeIDList(nodeIDList.length - 1)) {
            fingerNode = nodeIDList(0)
          } else if (start < nodeIDList(0)) {
            fingerNode = nodeIDList(0)
          } else {
            var j = 0
            while (j < (nodeIDList.length - 1)) {
              if (start > nodeIDList(j) && start < nodeIDList(j + 1)) {
                fingerNode = nodeIDList(j + 1)
              }
              j += 1

            }

          }

          fingerTable(fingerindex) = (start + "," + fingerNode)
        }

        if (index == 0) {
          var x = nodeIDList(nodeIDList.length - 1) + 1
          keys = List()
          for (i <- x to math.pow(2, m).toInt - 1) {
            keys ::= i
          }
          for (i <- 0 to nodeIDList(index)) {
            keys ::= i
          }
        } else if (index != 0 && index <= nodeIDList.length - 1) {
          keys = List()
          for (i <- nodeIDList(index - 1) + 1 to nodeIDList(index)) {
            keys ::= i
          }
        }
        //println(index)
        peerNodes(nodeIDList(index)) ! initNode(nodeIDList(index), successor, predecessor, fingerTable, m, numRequests, keys, hopActor)
      }
      for (i <- 0 to nodeIDList.length - 1) {
        peerNodes(nodeIDList(i)) ! informNodes(peerNodes)

      }

      println("Initial network Built..")
      println("Join Started..")
        self ! sendNextNode(0)
    }
    
    case sendNextNode(nIndex:Int) =>{
      peerNodes(joiningNode(nIndex)) = context.system.actorOf(Props(new nodeActor(m)))
        peerNodes(joiningNode(nIndex)) ! join(joiningNode(nIndex), peerNodes(nodeIDList(0)), peerNodes, numRequests, hopActor)
      
    }

  }

  def getKey(): Int = {
    val keyString = "nikhiltiware" + "parikshittiwari" + scala.util.Random.nextInt(256) + "." + scala.util.Random.nextInt(256) + "." + scala.util.Random.nextInt(256) + "." + scala.util.Random.nextInt(256)
    val md = java.security.MessageDigest.getInstance("SHA-1")
    var encodedString = md.digest(keyString.getBytes("UTF-8")).map("%02x".format(_)).mkString
    encodedString = new BigInteger(encodedString, 16).toString(2)
    var keyHash = Integer.parseInt(encodedString.substring(encodedString.length() - m), 2)
    if (nodeIDList.contains(keyHash)) {
      keyHash = getKey()
    }
    return keyHash
  }

}
object project3 extends App {
  if (args.length == 0 || args.length != 2) {
    println("Wrong Arguments");
  } else {
    var numNodes: Int = args(0).toInt
    var numRequests: Int = args(1).toInt
    val system = ActorSystem("NodesSystem")
    var nodeIDList: List[Int] = List()
    var m: Int = Math.ceil(Math.log(numNodes) / Math.log(2.0)).toInt
    var nodeSpace: Int = math.pow(2, m).toInt

    var joiningNode: List[Int] = List()
    def getNodeID(nodes: List[Int]): Int = {
      val nodeIP = scala.util.Random.nextInt(256) + "." + scala.util.Random.nextInt(256) + "." + scala.util.Random.nextInt(256) + "." + scala.util.Random.nextInt(256)
      val md = java.security.MessageDigest.getInstance("SHA-1")
      var encodedString = md.digest(nodeIP.getBytes("UTF-8")).map("%02x".format(_)).mkString
      encodedString = new BigInteger(encodedString, 16).toString(2)

      var addressHash = Integer.parseInt(encodedString.substring(encodedString.length() - m), 2)
      if (nodeIDList.contains(addressHash) || joiningNode.contains(addressHash)) {
        addressHash = getNodeID(nodes)
      }
      return addressHash
    }
    if(numNodes>=10){
     for (index <- 0 to numNodes-6) {
      nodeIDList ::= getNodeID(nodeIDList)
    }
    
    for (j <- 0 to 4) {
      joiningNode ::= getNodeID(nodeIDList)
    }      
    }else{
     for (index <- 0 to numNodes-3) {
      nodeIDList ::= getNodeID(nodeIDList)
    }
    
    for (j <- 0 to 1) {
      joiningNode ::= getNodeID(nodeIDList)
    } 
      
    }
    
    println("Number of Nodes:" + numNodes)
    println("Request per Node:" + numRequests)
    println()
    println("Network Build Started...")
    println("Joining Nodes: " + joiningNode.toList)
    //val tempList: List[Int] = 6:: 5:: 15:: 12:: 2:: Nil
    //val tempJoinList: List[Int] = 1::3::11::8:: 10:: Nil
    var master: ActorRef = system.actorOf(Props(new Master(m)))
    master ! InitMaster(nodeIDList, numRequests, m, joiningNode)
    
  }
}