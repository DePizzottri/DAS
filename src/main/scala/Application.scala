import scala.collection.immutable.List
import scala.io.StdIn
import scala.collection.mutable.Queue
import scala.collection.mutable.Set
import scala.io.Codec
import scala.concurrent.Future
import spray.json._
import DefaultJsonProtocol._
import java.util.NoSuchElementException

import com.mongodb.casbah.MongoClient
import com.mongodb.casbah.Imports._


object BfsApp extends App {
  object DBA {
    val mongoClient = MongoClient("192.168.1.9", 27017)
    val db = mongoClient("VKBFS")
    
    def insert(id:Int, friends:List[Int]) {
      db("ids").insert(MongoDBObject("id" -> id, "friends" -> friends))
    }
  }
  
  import scala.concurrent.ExecutionContext.Implicits.global
  
  def getList(id:Int) = {
    val response = scala.io.Source.fromURL(s"https://api.vk.com/method/friends.get?user_id=$id")(Codec.UTF8).mkString
    
    val jresp = response.parseJson
    
    try
    {
      jresp.asJsObject.getFields("response").head.convertTo[List[Int]]
    }
    catch {
      case c:NoSuchElementException => List()
    }      
  }
  
  def getListAsync(id:Int) = {
    Future { 
      getList(id)
    }
  }
  
  var q = new Queue[Int]
  var used = Set.empty[Int]
  
  q.enqueue(1)
  used += 1
  
  var cnt = 1
  while(!q.isEmpty) {
    val v = q.dequeue()
    
    println(v)
    
    val f = getList(v)
    
    DBA.insert(v, f)
    
    for (x <- f; if !used.contains(x)) yield {
      q.enqueue(x)
      used += x
    }
    
    cnt += 1
    if(cnt % 100 == 0)
      System.err.println(q.size)
  }  
}