package spi

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import org.iq80.leveldb.Options
import org.iq80.leveldb.ReadOptions
import org.iq80.leveldb.WriteOptions
import org.iq80.leveldb.impl.Iq80DBFactory
import pluginloader.api.caching
import pluginloader.api.nonNull
import java.io.File
import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.collections.HashMap
import kotlin.system.measureTimeMillis

internal class Database(dir: File, options: Options) {
    private val executor = Executors.newSingleThreadExecutor()
    private val database = Iq80DBFactory.factory.open(dir, options)
    private val readOptions = ReadOptions()
    private val locks = HashMap<UUID, HashMap<String, ByteArray>>()

    fun lock(uuid: UUID){
        submit {
            val uuidBuf = ByteBuffer.allocate(16)
            pushUUID(uuid, uuidBuf)
            val readingMap = HashMap<String, ByteArray>()
            caching {
                database.get(uuidBuf.array(), readOptions).nonNull{decode(it, readingMap)}
            }?.printStackTrace()
            locks[uuid] = readingMap
        }
    }

    fun unlock(uuid: UUID){
        submit {
            locks.remove(uuid).nonNull {
                val uuidBuf = ByteBuffer.allocate(16)
                pushUUID(uuid, uuidBuf)
                database.put(uuidBuf.array(), encode(it))
            }
        }
    }

    fun push(uuid: UUID, table: String, data: ByteArray){
        submit{
            locks[uuid].nonNull{
                it[table] = data
                return@submit
            }
            val uuidBuf = ByteBuffer.allocate(16)
            pushUUID(uuid, uuidBuf)
            val readingMap = HashMap<String, ByteArray>()
            caching {
                database.get(uuidBuf.array(), readOptions).nonNull{decode(it, readingMap)}
            }?.printStackTrace()
            readingMap[table] = data
            database.put(uuidBuf.array(), encode(readingMap), WriteOptions())
        }
    }

    fun drop(uuid: UUID, table: String){
        submit{
            locks[uuid].nonNull{
                it.remove(table)
                return@submit
            }
            val uuidBuf = ByteBuffer.allocate(16)
            pushUUID(uuid, uuidBuf)
            val readingMap = HashMap<String, ByteArray>()
            caching {
                database.get(uuidBuf.array(), readOptions).nonNull { decode(it, readingMap) }
            }?.printStackTrace()
            readingMap.remove(table)
            database.put(uuidBuf.array(), encode(readingMap))
        }
    }

    fun get(uuid: UUID, table: String, callback: (ByteArray?) -> Unit){
        submit{
            locks[uuid].nonNull{
                callback(it[table])
                return@submit
            }
            caching {
                val uuidBuf = ByteBuffer.allocate(16)
                pushUUID(uuid, uuidBuf)
                val readingMap = HashMap<String, ByteArray>()
                database.get(uuidBuf.array(), readOptions).nonNull { decode(it, readingMap) }
                callback(readingMap[table])
            }.nonNull{callback(null)}
        }
    }

    fun rewrite(dir: File){
        submit {
            val time = measureTimeMillis {
                val write = Database(dir, Options())
                database.forEach{write.database.put(it.key, it.value)}
                caching {write.close()}.nonNull(Throwable::printStackTrace)
            }
            println("Time to rewrite ${time}ms")
        }
    }

    fun close(){
        submit {
            locks.forEach{
                val uuid = it.key
                val uuidBuf = ByteBuffer.allocate(16)
                pushUUID(uuid, uuidBuf)
                caching{database.put(uuidBuf.array(), encode(it.value))}?.printStackTrace()
            }
            caching{database.close()}?.printStackTrace()
        }
        executor.shutdown()
        executor.awaitTermination(1, TimeUnit.DAYS)
    }

    fun delete(uuid: UUID){
        submit {
            locks.remove(uuid)
            val uuidBuf = ByteBuffer.allocate(16)
            pushUUID(uuid, uuidBuf)
            database.delete(uuidBuf.array())
        }
    }

    fun iterate(table: String, callback: (uuid: UUID, data: ByteArray) -> Unit, end: () -> Unit){
        submit {
            database.forEach{
                caching {
                    val uuidBuf = ByteBuffer.wrap(it.key)
                    val uuid = UUID(uuidBuf.long, uuidBuf.long)
                    val readingMap = HashMap<String, ByteArray>()
                    decode(it.value, readingMap)
                    callback(uuid, readingMap[table] ?: return@forEach)
                }?.printStackTrace()
            }
            end()
        }
    }

    private inline fun submit(crossinline invoke: () -> Unit){
        executor.submit{caching(invoke)?.printStackTrace()}
    }

    private fun encode(readingMap: MutableMap<String, ByteArray>): ByteArray{
        var size = 4 //map size
        readingMap.forEach{
            size += 4 + 4 //string size + data size
            size += it.key.toByteArray().size
            size += it.value.size
        }
        val byteBuf = ByteBuffer.allocate(size)
        byteBuf.putInt(readingMap.size)
        readingMap.forEach{
            val key = it.key.toByteArray()
            byteBuf.putInt(key.size).putInt(it.value.size).put(key).put(it.value)
        }
        return byteBuf.array()
    }

    private fun decode(data: ByteArray, readingMap: MutableMap<String, ByteArray>){
        try {
            val buf = ByteBuffer.wrap(data)
            val size = buf.int
            for(i in 0 until size){
                val key = ByteArray(buf.int)
                val value = ByteArray(buf.int)
                buf.get(key).get(value)
                readingMap[String(key)] = value
            }
        }catch (ex: Throwable){
            ex.printStackTrace()
        }
    }

    private fun pushUUID(uuid: UUID, uuidBuf: ByteBuffer){
        uuidBuf.position(0)
        uuidBuf.putLong(uuid.mostSignificantBits)
        uuidBuf.putLong(uuid.leastSignificantBits)
    }
}

internal class SpiImpl(private val database: Database, val table: String, val kSerializer: KSerializer<Any>): SPI<Any>{
    override fun save(uuid: UUID, data: Any) {
        database.push(uuid, table, Json.encodeToString(kSerializer, data).toByteArray())
    }

    override fun load(uuid: UUID, callback: (data: Any?) -> Unit) {
        database.get(uuid, table){
            if(it == null)callback(null)
            else callback(Json.decodeFromString(kSerializer, String(it)))
        }
    }

    override fun remove(uuid: UUID) {
        database.drop(uuid, table)
    }

    override fun iterate(callback: (uuid: UUID, data: Any) -> Unit, end: () -> Unit) {
        database.iterate(table, {uuid, data -> try{
            callback(uuid, Json.decodeFromString(kSerializer, String(data)))
        }catch (ex: Throwable){
            ex.printStackTrace()
        }}, {plu.task(end)})
    }
}