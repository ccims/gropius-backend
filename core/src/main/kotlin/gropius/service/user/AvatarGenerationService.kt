package gropius.service.user


import org.springframework.stereotype.Service
import java.util.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.pow

private const val SIZE = 36

/**
 * Service to generate avatars for users
 * Based upon https://github.com/boringdesigners/boring-avatars
 */
@Service
class AvatarGenerationService {

    /**
     * The default color palette
     */
    private val colors = listOf("#001449","#012677","#005bc5","#00b4fc","#17f9ff")

    /**
     * Generates a random avatar for the given id
     * Avatar is returned as Base64 encoded SVG data url
     *
     * @param id The id of the user
     * @return The avatar as Base64 encoded SVG data url
     */
    fun generateAvatar(id: String): String {
        val numFromName = max(abs(id.hashCode()), 0)
        val range = colors.size
        val preTranslateX = getUnit(numFromName, 10, 1)
        val wrapperTranslateX = if (preTranslateX < 5) preTranslateX + SIZE / 9.0 else preTranslateX.toDouble()
        val preTranslateY = getUnit(numFromName, 10, 2)
        val wrapperTranslateY = if (preTranslateY < 5) preTranslateY + SIZE / 9.0 else preTranslateY.toDouble()

        val wrapperColor = getRandomColor(numFromName, colors, range)
        val faceColor = getContrast(wrapperColor)
        val backgroundColor = getRandomColor(numFromName + 13, colors, range)
        val wrapperRotate = getUnit(numFromName, 360, 0)
        val wrapperScale = 1 + getUnit(numFromName, SIZE / 12, 0) / 10.0
        val isMouthOpen = getBoolean(numFromName, 3)
        val isCircle = getBoolean(numFromName, 1)
        val eyeSpread = getUnit(numFromName, 5, 0)
        val mouthSpread = getUnit(numFromName, 3, 0)
        val faceRotate = getUnit(numFromName, 10, 3)
        val faceTranslateX = if (wrapperTranslateX > SIZE / 6.0) wrapperTranslateX / 2.0 else getUnit(numFromName, 8, 1)
        val faceTranslateY = if (wrapperTranslateY > SIZE / 6.0) wrapperTranslateY / 2.0 else getUnit(numFromName, 7, 2)

        val svg = Tag("svg") {
            attrs["xmlns"] = "http://www.w3.org/2000/svg"
            attrs["viewBox"] = "0 0 $SIZE $SIZE"

            tag("g") {
                tag("rect") {
                    attrs["width"] = SIZE.toString()
                    attrs["height"] = SIZE.toString()
                    attrs["fill"] = backgroundColor
                }
                tag("rect") {
                    attrs["x"] = "0"
                    attrs["y"] = "0"
                    attrs["width"] = SIZE.toString()
                    attrs["height"] = SIZE.toString()
                    attrs["transform"] = "translate($wrapperTranslateX $wrapperTranslateY) rotate($wrapperRotate ${SIZE / 2} ${SIZE / 2}) scale($wrapperScale)"
                    attrs["fill"] = wrapperColor
                    attrs["rx"] = if (isCircle) SIZE.toString() else (SIZE / 6).toString()
                }
                tag("g") {
                    attrs["transform"] = "translate($faceTranslateX $faceTranslateY) rotate($faceRotate ${SIZE / 2} ${SIZE / 2})"
                    if (isMouthOpen) {
                        tag("path") {
                            attrs["d"] = "M15 ${19 + mouthSpread} c2 1 4 1 6 0"
                            attrs["stroke"] = faceColor
                            attrs["fill"] = "none"
                            attrs["stroke-linecap"] = "round"
                        }
                    } else {
                        tag("path") {
                            attrs["d"] = "M13 ${19 + mouthSpread} a1,0.75 0 0,0 10,0"
                            attrs["fill"] = faceColor
                        }
                    }
                    tag("rect") {
                        attrs["x"] = (14 - eyeSpread).toString()
                        attrs["y"] = "14"
                        attrs["width"] = "1.5"
                        attrs["height"] = "2"
                        attrs["fill"] = faceColor
                        attrs["rx"] = "1"
                        attrs["stroke"] = "none"
                    }
                    tag("rect") {
                        attrs["x"] = (20 + eyeSpread).toString()
                        attrs["y"] = "14"
                        attrs["width"] = "1.5"
                        attrs["height"] = "2"
                        attrs["fill"] = faceColor
                        attrs["rx"] = "1"
                        attrs["stroke"] = "none"
                    }
                }
            }
        }
        val svgString =  svg.toString()
        return "data:image/svg+xml;base64,${Base64.getEncoder().encodeToString(svgString.toByteArray())}"
    }

    /**
     * Gets a random color from the provided list of colors based on the number
     *
     * @param number The number to use to get the color
     * @param colors The list of colors to choose from
     * @param range The range of colors to choose from
     * @return The chosen color
     */
    private fun getRandomColor(number: Int, colors: List<String>, range: Int): String {
        return colors[number % range]
    }

    /**
     * Generates a number required for rendering the image
     *
     * @param number Required for generating the number
     * @param range Required for generating the number
     * @param index Required for generating the number
     * @return The generated number
     */
    private fun getUnit(number: Int, range: Int, index: Int): Int {
        val value = number % range
        return if (index != 0 && getDigit(number, index) % 2 == 0) {
            -value
        } else {
            value
        }
    }

    /**
     * Gets the nth digit of a number, starting from the right
     *
     * @param number The number to get the digit from
     * @param nth The index of the digit to get
     * @return The digit at the nth index
     */
    private fun getDigit(number: Int, nth: Int): Int {
        return (number / 10.0.pow(nth)).toInt() % 10
    }

    /**
     * Gets the contrast color for a given color
     *
     * @param color The color to get the contrast for
     * @return The contrast color
     */
    private fun getContrast(color: String): String {
        val r = color.substring(1, 3).toInt(16)
        val g = color.substring(3, 5).toInt(16)
        val b = color.substring(5, 7).toInt(16)
        val yiq = (r * 299 + g * 587 + b * 114) / 1000
        return if (yiq >= 128) "#000000" else "#ffffff"
    }

    /**
     * Gets whether the nth digit of a number is even
     *
     * @param number The number to get the digit from
     * @param nth The index of the digit to get
     * @return Whether the nth digit is even
     */
    private fun getBoolean(number: Int, nth: Int): Boolean {
        return getDigit(number, nth) % 2 == 0
    }
}

/**
 * Simple DSL for generating SVGs
 *
 * @param name The name of the element
 * @param block The block to execute, used for defining attributes and inner elements
 */
private class Tag(val name: String, block: Tag.() -> Unit) {
    /**
     * The attributes of the tag
     */
    val attrs = mutableMapOf<String, String>()

    /**
     * The inner tags of the tag
     */
    private val tags = mutableListOf<Tag>()

    init {
        block()
    }

    /**
     * Adds a new inner element
     *
     * @param name The name of the element
     * @param block The block defining the inner element
     */
    fun tag(name: String, block: Tag.() -> Unit) {
        val tag = Tag(name, block)
        tags.add(tag)
    }

    override fun toString(): String {
        val attributes = attrs.map { "${it.key}=\"${it.value}\"" }.joinToString(" ")
        val innerTags = tags.joinToString("")
        return "<$name $attributes>$innerTags</$name>"
    }
}