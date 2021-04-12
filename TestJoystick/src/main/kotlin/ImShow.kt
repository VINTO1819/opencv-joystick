package com.atul.JavaOpenCV

import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import java.awt.Dimension
import java.awt.image.BufferedImage
import java.awt.image.DataBufferByte
import java.lang.Exception
import javax.swing.ImageIcon
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.WindowConstants

/*
* Author: ATUL
* Thanks to Daniel Baggio , Jan Monterrubio and sutr90 for improvements
* This code can be used as an alternative to imshow of OpenCV for JAVA-OpenCv
* Make sure OpenCV Java is in your Build Path
* Usage :
* -------
* Imshow ims = new Imshow("Title");
* ims.showImage(Mat image);
* Check Example for usage with Webcam Live Video Feed
*/ //import java.io.ByteArrayInputStream;
//import java.io.InputStream;
//import javax.imageio.ImageIO;
//import javax.swing.plaf.ButtonUI;
class Imshow {
    var Window: JFrame
    private var image: ImageIcon
    private var label: JLabel

    // private MatOfByte matOfByte;
    private var SizeCustom: Boolean
    private var Height = 0
    private var Width = 0

    constructor(title: String?) {
        Window = JFrame()
        image = ImageIcon()
        label = JLabel()
        // matOfByte = new MatOfByte();
        label.icon = image
        Window.contentPane.add(label)
        Window.isResizable = false
        Window.title = title
        SizeCustom = false
        setCloseOption(0)
    }

    constructor(title: String?, height: Int, width: Int) {
        SizeCustom = true
        Height = height
        Width = width
        Window = JFrame()
        image = ImageIcon()
        label = JLabel()
        // matOfByte = new MatOfByte();
        label.icon = image
        Window.contentPane.add(label)
        Window.isResizable = false
        Window.title = title
        setCloseOption(0)
    }

    fun showImage(img: Mat) {
        if (SizeCustom) {
            Imgproc.resize(img, img, Size(Height.toDouble(), Width.toDouble()))
        }
        // Highgui.imencode(".jpg", img, matOfByte);
        // byte[] byteArray = matOfByte.toArray();
        var bufImage: BufferedImage? = null
        try {
            // InputStream in = new ByteArrayInputStream(byteArray);
            // bufImage = ImageIO.read(in);
            bufImage = toBufferedImage(img)
            image.image = bufImage
            Window.pack()
            label.updateUI()
            Window.isVisible = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // CREDITS TO DANIEL: http://danielbaggio.blogspot.com.br/ for the improved
    // version !
    fun toBufferedImage(m: Mat): BufferedImage {
        var type = BufferedImage.TYPE_BYTE_GRAY
        if (m.channels() > 1) {
            type = BufferedImage.TYPE_3BYTE_BGR
        }
        val bufferSize = m.channels() * m.cols() * m.rows()
        val b = ByteArray(bufferSize)
        m[0, 0, b] // get all the pixels
        val image = BufferedImage(m.cols(), m.rows(), type)
        val targetPixels = (image.raster
            .dataBuffer as DataBufferByte).data
        System.arraycopy(b, 0, targetPixels, 0, b.size)
        return image
    }

    // Thanks to sutr90 for reporting the issue : https://github.com/sutr90
    fun setCloseOption(option: Int) {
        when (option) {
            0 -> Window.defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
            1 -> Window.defaultCloseOperation = WindowConstants.HIDE_ON_CLOSE
            else -> Window.defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
        }
    }

    /**
     * Sets whether this window should be resizable or not, by default it is not
     * resizable
     *
     * @param resizable
     * `true` if the window should be resizable,
     * `false` otherwise
     */
    fun setResizable(resizable: Boolean) {
        Window.isResizable = resizable
    }

    companion object {
        /**
         * Displays the given [Mat] in a new instance of [Imshow] with
         * the given title as the title for the window
         *
         * @param mat
         * the [Mat] to display
         * @param frameTitle
         * the title for the frame
         */
        fun show(mat: Mat, frameTitle: String?) {
            show(
                mat, Dimension(mat.rows(), mat.cols()), frameTitle, false,
                WindowConstants.EXIT_ON_CLOSE
            )
        }

        /**
         * Displays the given [Mat] in a new instance of [Imshow] with
         * the given title as the title for the window and determines whether the
         * frame is resizable or not
         *
         * @param mat
         * the [Mat] to display
         * @param frameTitle
         * the title for the frame
         * @param resizable
         * whether the frame should be resizable or not
         */
        fun show(mat: Mat, frameTitle: String?, resizable: Boolean) {
            show(
                mat, Dimension(mat.rows(), mat.cols()), frameTitle, resizable,
                WindowConstants.EXIT_ON_CLOSE
            )
        }
        /**
         * Displays the given [Mat] in a new instance of [Imshow] with a
         * set size and given title and whether it is resizable or not, and with the
         * close operation set
         *
         * @param mat
         * the [Mat] to display
         * @param frameSize
         * the size for the frame
         * @param frameTitle
         * the title for the frame
         * @param resizable
         * wether the frame is resizable or not
         * @param closeOperation
         * the constant for the default close operation of the frame
         */
        // Thanks to Jan Monterrubio for additional static methods for viewing images.
        /**
         * Displays the given [Mat] in a new instance of [Imshow]
         *
         * @param mat
         * the [Mat] to display
         */
        /**
         * Displays the given [Mat] in a new instance of [Imshow] with a
         * set size
         *
         * @param mat
         * the [Mat] to display
         * @param frameSize
         * the size for the frame
         */
        /**
         * Displays the given [Mat] in a new instance of [Imshow] with a
         * set size and given title
         *
         * @param mat
         * the [Mat] to display
         * @param frameSize
         * the size for the frame
         * @param frameTitle
         * the title for the frame
         */
        /**
         * Displays the given [Mat] in a new instance of [Imshow] with a
         * set size and given title and whether it is resizable or not
         *
         * @param mat
         * the [Mat] to display
         * @param frameSize
         * the size for the frame
         * @param frameTitle
         * the title for the frame
         */
        @JvmOverloads
        fun show(
            mat: Mat, frameSize: Dimension = Dimension(mat.rows(), mat.cols()), frameTitle: String? = "",
            resizable: Boolean = false, closeOperation: Int =
                WindowConstants.EXIT_ON_CLOSE
        ) {
            val frame = Imshow(frameTitle, frameSize.height, frameSize.width)
            frame.setResizable(resizable)

            /*
		 * This is a bad way to access the window, but due to legacy stuff I
		 * won't change the access patterns
		 */frame.Window.defaultCloseOperation = closeOperation
            frame.showImage(mat)
        }
    }
}