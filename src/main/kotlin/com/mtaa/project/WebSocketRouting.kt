package com.mtaa.project

import io.ktor.http.cio.websocket.*
import io.ktor.routing.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.SendChannel
import java.lang.Exception

var connectedClients : HashMap<Int,MutableList<SendChannel<Frame>>> = HashMap()

fun Route.websocketRouting() {
    webSocket("/votes") {
        for (frame in incoming) {
            when (frame) {
                is Frame.Text -> {
                    val text = frame.readText()

                    println("GOT $text")
                    val recieved = text.split(" ")
                    if(recieved.size <=1)
                        return@webSocket

                    val id = recieved[1].toInt()    //Review ID

                    if(connectedClients[id] != null) {
                        println("Adding")
                        connectedClients[id]?.add(outgoing)
                    }
                    else {
                        println("Creating")
                        connectedClients[id] = mutableListOf(outgoing)
                    }

                    outgoing.send(Frame.Text("CONNECTED"))
                }
            }
        }
    }
}

@ExperimentalCoroutinesApi
fun sendVotesUpdate(id: Int, votes: ReviewVotesInfo) {
    println("Called $id")

    CoroutineScope(Dispatchers.IO).launch {
        if (connectedClients[id] != null) {
            for (item in connectedClients[id]!!) {
                //Delete closed connection
                if (item.isClosedForSend) {
                    println("Remove close connection")
                    connectedClients[id]?.remove(item)
                    continue
                }

                //Send votes data
                try {
                    println("Sending data")
                    item.send(Frame.Text("${votes.likes} ${votes.dislikes}"))
                } catch (e: Exception) {
                    println(e.stackTraceToString())
                }
            }
            println("Done sending")
        }
        else {
            println("Null hashmap")
        }
    }
}