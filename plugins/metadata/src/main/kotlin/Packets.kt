package metadata

import kotlinx.serialization.Serializable
import tower.api.Packet

@Serializable
internal class Push(val name: String, val data: String): Packet

@Serializable
internal class Drop(val name: String, val data: String): Packet

@Serializable
internal class All(val name: String): Packet