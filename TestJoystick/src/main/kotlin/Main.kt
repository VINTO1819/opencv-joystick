import com.atul.JavaOpenCV.Imshow
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import org.opencv.videoio.VideoCapture

fun main(args:Array<String>){
    System.loadLibrary(Core.NATIVE_LIBRARY_NAME)

    val gui_Process = Imshow("Test Joystick for processing")
    val gui_Show = Imshow("Test JoyStick")
    val cam = VideoCapture(0)

    while(true){
        // get Frame
        val img = Mat()
        cam.read(img)

        /*val blurImg = Mat()
        val siz = 15.0
        Imgproc.GaussianBlur(img, blurImg, Size(siz, siz),0.0)*/

        // Skin detection
        val hsvImage = Mat()
        Imgproc.cvtColor(img, hsvImage, Imgproc.COLOR_BGR2HSV)
        // G : 20~225 | R : 150~225 | B : 0~20
        val lower = Scalar(0.0, 15.0, 125.0)
        val upper = Scalar(20.0, 225.0, 225.0)
        val skinImage = Mat()
        Core.inRange(hsvImage, lower, upper, skinImage)

        val handMask = Mat()

        val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, Size(3.0, 3.0))
        Imgproc.morphologyEx(skinImage, handMask, Imgproc.MORPH_CLOSE, kernel)
        Imgproc.dilate(handMask, handMask, kernel)

        val siz = 5.0
        Imgproc.GaussianBlur(handMask, handMask, Size(siz, siz),0.0)

        val hand = Mat()
        Core.bitwise_and(img, img, hand, handMask)

        // find hands
        val contours = mutableListOf<MatOfPoint>()
        val hieraray = Mat()
        Imgproc.findContours(handMask, contours, hieraray, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE)

        val foundForProcess = Mat(hand.size(), 0)
        val foundForShow = img.clone()

        val cnt = contours
            .filter{Imgproc.contourArea(it) in 7500.0..35000.0}
            .filter{
                val rect = Imgproc.boundingRect(it)
                val ratio = rect.width.toDouble() / rect.height.toDouble()
                println(ratio)
                ratio in 0.5..1.5
            }
            .sortedByDescending {
                val rect = Imgproc.boundingRect(it)
                rect.y
            }

        val rawHands = mutableListOf<MatOfPoint>()

        for(ct in cnt){
            Imgproc.drawContours(foundForProcess, listOf(ct), -1, Scalar(255.0, 0.0, 0.0))
            val isHand = cnt.indexOf(ct) in 0..1
            if(isHand) rawHands.add(ct)

            val color = if(isHand){Scalar(0.0, 000.0, 128.0)}else{Scalar(0.0, 128.0, 0.0)}

            val rect = Imgproc.boundingRect(ct)
            Imgproc.rectangle(foundForShow, rect.tl(), rect.br(), color)
            Imgproc.putText(foundForShow, "Size : ${Imgproc.contourArea(ct)}", Point(rect.tl().x, rect.br().y), Core.FONT_HERSHEY_COMPLEX_SMALL, 1.0, color)
            println("width : ${rect.width} / height : ${rect.height} / size : ${rect.height * rect.width}")
        }

        // 손 처리하기
        val hands = rawHands.toList().sortedBy { Imgproc.boundingRect(it).x }
        //val centerPoint = Point(foundForShow.size().width / 2, foundForShow.size().height / 2)
        val centerSize = 15000 // 중립 상태에서의 손 크기(평균 산정)

        hands.forEach{
            val handRect = Imgproc.boundingRect(it)
            val centerOfHand = Point((handRect.x + handRect.width / 2).toDouble(), (handRect.y + handRect.height / 2).toDouble())
            Imgproc.circle(foundForShow, centerOfHand, 1, Scalar(0.0, 0.0, 128.0), 5)
        }

        if(hands.size == 2){
            val leftHand = Imgproc.boundingRect(hands[0])
            val rightHand = Imgproc.boundingRect(hands[1])
            val slopeRaw:Double = -((rightHand.y - leftHand.y) / Math.abs(rightHand.x - leftHand.x).toDouble())

            val maximiumAnalog = 1024 // 조이스틱 최대값(절대)
            val maximiumRaw = 0.6 // slopeRaw의 최대값
            val percent = (slopeRaw / maximiumRaw * 100) * 0.6

            var analogSlope = maximiumAnalog * percent / 100 // 조이스틱에 들어가는 아날로그 값
            // 오차 방지를 위해 일정 범위값은 0으로 처리
            if(analogSlope in -30.0..30.0){ analogSlope = 0.0 }

            Imgproc.putText(foundForShow, "Slope : ${analogSlope}", Point(0.0, 30.0), Core.FONT_HERSHEY_COMPLEX, 1.0, Scalar(128.0, 0.0, 0.0))
        }

        // 결과물 출력
        gui_Process.showImage(foundForProcess)
        gui_Show.showImage(foundForShow)
    }

}
