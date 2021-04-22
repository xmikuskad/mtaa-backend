package com.mtaa.project

import io.ktor.http.cio.websocket.*
import io.ktor.routing.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.SendChannel
import java.lang.Exception

var votesClients : HashMap<Int,MutableList<SendChannel<Frame>>> = HashMap()
var mainMenuClients : MutableList<SendChannel<Frame>> = mutableListOf()

const val VOTES_COMMAND = "votes"
const val MENU_COMMAND = "menu"
const val LOAD_COMMAND = "load"

fun Route.websocketRouting() {
    webSocket("/votes") {
        for (frame in incoming) {
            when (frame) {
                is Frame.Text -> {
                    val text = frame.readText()

                    val recieved = text.split(" ")
                    if (recieved.size != 2 && recieved[0] != VOTES_COMMAND) {
                        return@webSocket
                    }

                    var id: Int
                    try {
                        id = recieved[1].toInt()    //Review ID
                    } catch (e: Exception) {
                        println(e.stackTraceToString())
                        outgoing.send(Frame.Text("WRONG ID"))
                        return@webSocket
                    }

                    if (votesClients[id] != null) {
                        votesClients[id]?.add(outgoing)
                    } else {
                        votesClients[id] = mutableListOf(outgoing)
                    }

                    //This is not mandatory
                    outgoing.send(Frame.Text("CONNECTED"))
                }
            }
        }
    }

    webSocket("/reviews") {
        for (frame in incoming) {
            when (frame) {
                is Frame.Text -> {
                    val text = frame.readText()
                    val recieved = text.split(" ")

                    if(recieved[0] == MENU_COMMAND) {
                        mainMenuClients.add(outgoing)
                    }

                    //This is not mandatory
                    outgoing.send(Frame.Text("CONNECTED"))
                }
            }
        }
    }
}

//Called after likes/dislike update
@ExperimentalCoroutinesApi
fun sendVotesUpdate(id: Int, votes: ReviewVotesInfo) {
    CoroutineScope(Dispatchers.IO).launch {
        if (votesClients[id] != null) {
            //Delete closed connection
            votesClients[id]!!.removeAll {it.isClosedForSend}
            for (item in votesClients[id]!!) {
                //Send votes data
                try {
                    item.send(Frame.Text("${votes.likes} ${votes.dislikes}"))
                } catch (e: Exception) {
                    println(e.stackTraceToString())
                }
            }
        }
    }
}

//Called after review update, delete or add
@ExperimentalCoroutinesApi
fun sendRecentReviewUpdate() {
    CoroutineScope(Dispatchers.IO).launch {
        //Delete closed connection
        mainMenuClients.removeAll {it.isClosedForSend}

        for (item in mainMenuClients) {
            //Send votes data
            try {
                item.send(Frame.Text(LOAD_COMMAND))
            } catch (e: Exception) {
                println(e.stackTraceToString())
            }
        }
    }
}