import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
public interface Message

public data class GetMessage(val versions: List<Long>) : Message

public class GetAllMessage : Message

public data class MessageBatch(val messages: List<Message>) : Message

public interface VersionedMessage : Message {
    val version: Long
}

public data class AddPointMessage(override val version: Long,
                                  val x: Double,
                                  val y: Double,
                                  val color: Int) : VersionedMessage

public data class AddLineMessage(override val version: Long,
                                 val x0: Double,
                                 val y0: Double,
                                 val x1: Double,
                                 val y1: Double,
                                 val color: Int) : VersionedMessage

public data class AddTextMessage(override val version: Long,
                                 val x: Double,
                                 val y: Double,
                                 val text: String,
                                 val color: Int,
                                 val fontSize: Int) : VersionedMessage

public data class MoveAreaMessage(override val version: Long,
                                  val x0: Double,
                                  val y0: Double,
                                  val x1: Double,
                                  val y1: Double) : VersionedMessage

