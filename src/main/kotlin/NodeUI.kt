import utils.Area
import utils.Point
import visualizer.PointDrawable
import visualizer.SegmentDrawable
import visualizer.TextDrawable
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import java.text.NumberFormat
import java.util.*
import javax.swing.*
import kotlin.concurrent.thread

/**
 * Created by igushs on 12/17/15.
 */

public class NodeUI() {

    private val node = Node { handleVersionedMessage(it) }

    val v = visualizer.Visualizer()
    val window: JFrame

    val checkBoxAreaReceive = JCheckBox("Follow area changes", true).apply {
        addItemListener {
            if (isSelected) {
                node.board.messages[lastAreaVersion]?.let {
                    val msg = it.findLast { it is MoveAreaMessage } as MoveAreaMessage
                    v.changeArea(Area(msg.x0, msg.y0, msg.x1, msg.y1))
                }
            }
        }
    }

    val fontSizeTextField = JFormattedTextField(NumberFormat.getIntegerInstance()).apply {
        value = 6L
        setFocusLostBehavior(JFormattedTextField.COMMIT_OR_REVERT)
        addKeyListener(object : KeyListener {
            override fun keyPressed(e: KeyEvent?) {
                if (this@apply.isEditValid)
                    this@apply.commitEdit()
            }

            override fun keyReleased(e: KeyEvent?) = Unit
            override fun keyTyped(e: KeyEvent?) = Unit
        })
        addFocusListener(object : FocusListener {
            override fun focusGained(e: FocusEvent?) {
                SwingUtilities.invokeLater {
                    selectAll()
                }
            }

            override fun focusLost(e: FocusEvent?) = Unit
        })
        addActionListener { SwingUtilities.invokeLater { selectAll() } }
    }

    val colorsList = arrayOf("#FF0000: red", "#FFFF00: yellow", "#00FF00: green", "#00FFFF: cyan", "#0000FF: blue", "#FF00FF: purple")
    val checkBoxAreaSend = JCheckBox("Send area updates", true)
    val colorComboBox = JComboBox<String>(colorsList).apply {
        addItemListener {
            val firstPart = it.item.toString().split(":")[0]
            setColor(firstPart, updateComboBox = false)
        }
        isEditable = true
    }
    var color = Random()
            .let { Color(128 + it.nextInt(128), 128 + it.nextInt(128), 128 + it.nextInt(128)) }
            .apply { setColor(colorToHex(this)) }

    private fun colorToHex(c: Color) = "#%02x%02x%02x".format(c.red, c.green, c.blue)
    private fun setColor(s: String, updateComboBox: Boolean = true) {
        try {
            val c = Color.decode(s)
            color = c
            if (updateComboBox)
                colorComboBox.selectedItem = colorToHex(c)
        } catch (e: NumberFormatException) {
            JOptionPane.showMessageDialog(window, "Bad color. Use HEX format")
        }
    }

    init {
        window = JFrame("Demo")
        with(window) {
            this.minimumSize = Dimension(600, 600)
            layout = BorderLayout()
            add(v, BorderLayout.CENTER)

            val controls = JPanel()
            controls.apply {
                layout = BoxLayout(controls, BoxLayout.LINE_AXIS)
                add(checkBoxAreaReceive)
                add(checkBoxAreaSend)
                add(Box.createRigidArea(Dimension(10, 5)))
                add(colorComboBox)
                add(Box.createRigidArea(Dimension(10, 5)))
                add(JLabel("Font size"))
                add(fontSizeTextField)
            }


            add(controls, BorderLayout.NORTH)

            defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
            setLocationRelativeTo(null)
        }
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
    }


    fun start() {

        node.start();

        window.isVisible = true

        thread { sendAreaLoop() }

        v.onClick { x, y ->
            node.sendMessage(AddPointMessage(node.board.nextVersion(), x, y, color.rgb))
        }
        v.onDrag { x0, y0, x1, y1 ->
            node.sendMessage(AddLineMessage(node.board.nextVersion(), x0, y0, x1, y1, color.rgb))
        }
        v.onRightClick { x, y ->
            val text = JOptionPane.showInputDialog("Enter text")
            if (text != null)
                node.sendMessage(AddTextMessage(node.board.nextVersion(), x, y, text,
                                                color.rgb, (fontSizeTextField.value as Long).toInt()))
        }
    }

    @Volatile
    var lastAreaVersion: Long = 0L

    private var lastAreaSent: Area = v.area

    /**
     * This is used to avoid spamming with area updates.
     * It just sends an update once per interval if the area changed.
     */
    private fun sendAreaLoop() {
        while (true) {
            val a = v.area
            if (checkBoxAreaSend.isSelected && lastAreaSent.toString() != a.toString()) {
                val msg = MoveAreaMessage(node.board.nextVersion(),
                                          a.lowerLeft.x,
                                          a.lowerLeft.y,
                                          a.upperRight.x,
                                          a.upperRight.y)
                lastAreaVersion = msg.version
                node.sendMessage(msg)
                lastAreaSent = a
            }
            Thread.sleep(50)
        }
    }

    private fun handleVersionedMessage(msg: Message) {
        when (msg) {
            is AddPointMessage -> {
                v.add(PointDrawable(msg.x, msg.y, Color(msg.color)))
            }
            is AddLineMessage -> {
                v.add(SegmentDrawable(Point(msg.x0, msg.y0), Point(msg.x1, msg.y1), Color(msg.color)))
            }
            is AddTextMessage -> {
                v.add(TextDrawable(Point(msg.x, msg.y), msg.text, Color(msg.color), msg.fontSize))
            }
            is MoveAreaMessage -> {
                if (msg.version > lastAreaVersion && checkBoxAreaReceive.isSelected) {
                    v.changeArea(Area(msg.x0, msg.y0, msg.x1, msg.y1))
                    lastAreaSent = v.area
                }
                lastAreaVersion = lastAreaVersion.coerceAtLeast(msg.version)
            }
        }
    }

}


fun main(args: Array<String>) {
    NodeUI().start()
}
