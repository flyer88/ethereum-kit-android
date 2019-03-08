package io.horizontalsystems.ethereumkit.spv.net.les

import io.horizontalsystems.ethereumkit.spv.crypto.ECKey
import io.horizontalsystems.ethereumkit.spv.models.BlockHeader
import io.horizontalsystems.ethereumkit.spv.net.IMessage
import io.horizontalsystems.ethereumkit.spv.net.INetwork
import io.horizontalsystems.ethereumkit.spv.net.MessageFactory
import io.horizontalsystems.ethereumkit.spv.net.Node
import io.horizontalsystems.ethereumkit.spv.net.devp2p.Capability
import io.horizontalsystems.ethereumkit.spv.net.devp2p.DevP2PPeer
import io.horizontalsystems.ethereumkit.spv.net.les.messages.*


class LESPeer(private val devP2PPeer: DevP2PPeer,
              private val messageFactory: MessageFactory,
              private val statusHandler: StatusHandler) : DevP2PPeer.Listener {

    interface Listener {
        fun didConnect()
        fun didReceive(blockHeaders: List<BlockHeader>)
        fun didReceive(message: ProofsMessage)
    }

    var listener: Listener? = null

    private fun handle(message: IMessage) {
        when (message) {
            is StatusMessage -> handle(message)
            is BlockHeadersMessage -> handle(message)
            is ProofsMessage -> handle(message)
        }
    }

    private fun handle(message: StatusMessage) {
        try {
            statusHandler.validate(message)
            listener?.didConnect()
        } catch (error: Exception) {
            disconnect(error)
        }
    }

    private fun handle(message: BlockHeadersMessage) {
        listener?.didReceive(message.headers)
    }

    private fun handle(message: ProofsMessage) {
        listener?.didReceive(message)
    }

    //------------------Public methods----------------------

    fun connect() {
        devP2PPeer.connect()
    }

    fun disconnect(error: Throwable?) {
        devP2PPeer.disconnect(error)
    }

    fun requestBlockHeadersFrom(blockHash: ByteArray) {
        val message = messageFactory.getBlockHeadersMessage(blockHash)
        devP2PPeer.send(message)
    }

    fun requestProofs(address: ByteArray, blockHash: ByteArray) {
        val message = messageFactory.getProofsMessage(address, blockHash)
        devP2PPeer.send(message)
    }

    //-----------DevP2PPeer.Listener methods------------

    override fun didConnect() {
        println("LESPeer -> didConnect\n")
        val statusMessage = messageFactory.statusMessage(statusHandler.network, statusHandler.blockHeader)
        devP2PPeer.send(statusMessage)
    }

    override fun didDisconnect(error: Throwable?) {
        println("LESPeer -> didDisconnect")
    }

    override fun didReceive(message: IMessage) {
        println("LESPeer -> didReceive")
        handle(message)
    }

    open class LESPeerError : Exception() {
        class WrongNetwork : LESPeerError()
        class InvalidBestBlockHeight : LESPeerError()
        class ExpiredBestBlockHeight : LESPeerError()
    }

    companion object {
        val capability = Capability("les", 2,
                hashMapOf(0x00 to StatusMessage::class,
                        0x02 to GetBlockHeadersMessage::class,
                        0x03 to BlockHeadersMessage::class,
                        0x0f to GetProofsMessage::class,
                        0x10 to ProofsMessage::class))

        fun getInstance(network: INetwork, bestBlock: BlockHeader, key: ECKey, node: Node): LESPeer {
            val devP2PPeer = DevP2PPeer.getInstance(key, node, listOf(capability))
            val statusHandler = StatusHandler(network, bestBlock)
            val lesPeer = LESPeer(devP2PPeer, MessageFactory(), statusHandler)

            devP2PPeer.listener = lesPeer

            return lesPeer
        }

    }
}
