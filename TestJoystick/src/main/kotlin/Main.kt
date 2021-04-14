import com.atul.JavaOpenCV.Imshow
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import org.opencv.videoio.VideoCapture
import java.awt.Robot
import java.awt.Toolkit

fun main(args:Array<String>){
    System.loadLibrary(Core.NATIVE_LIBRARY_NAME)

    val debugMode = readLine()!! == "dbg"

    val gui_Process = Imshow("Test Joystick for processing")
    val gui_Show = Imshow("Test JoyStick")
    val cam = VideoCapture(0)
    val controller = Robot()

    var lastPosition:Point? = null
    var ignoreCount = 0

    var using = true
    var lastTime = System.currentTimeMillis().toInt()
    var lastedChange = System.currentTimeMillis().toInt()
    var usingSwitchTime = 0

    while(true){
        // get Frame
        val img = Mat()
        cam.read(img)

        val roiSizeFor = Rect(Point((img.size().width / 5) * 1, img.size().height / 3), Point((img.size().width / 5) * 4, img.size().height))
        val roiImage = img.submat(roiSizeFor) /*img.adjustROI(
            ((img.size().width / 3) * 1).toInt(),
            0,
            ((img.size().width / 3) * 2).toInt(),
            img.size().height.toInt()
        )*/
        img.release()

        /*val blurImg = Mat()
        val siz = 15.0
        Imgproc.GaussianBlur(img, blurImg, Size(siz, siz),0.0)*/

        // Skin detection
        val hsvImage = Mat()
        Imgproc.cvtColor(roiImage, hsvImage, Imgproc.COLOR_BGR2HSV)
        // G : 20~225 | R : 150~225 | B : 0~20
        val lower = Scalar(0.0, 20.0, 140.0)
        val upper = Scalar(20.0, 225.0, 225.0)
        val skinImage = Mat()
        Core.inRange(hsvImage, lower, upper, skinImage)
        hsvImage.release()

        val handMask = Mat()

        val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, Size(3.0, 3.0))
        Imgproc.morphologyEx(skinImage, handMask, Imgproc.MORPH_CLOSE, kernel)
        Imgproc.dilate(handMask, handMask, kernel)
        skinImage.release()
        kernel.release()

        val siz = 5.0
        Imgproc.GaussianBlur(handMask, handMask, Size(siz, siz),0.0)

        val hand = Mat()
        Core.bitwise_and(roiImage, roiImage, hand, handMask)

        // find hands
        val contours = mutableListOf<MatOfPoint>()
        val hieraray = Mat()
        Imgproc.findContours(handMask, contours, hieraray, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE)
        handMask.release()
        hieraray.release()

        val foundForProcess = Mat(hand.size(), 0)
        val foundForShow = roiImage.clone()

        val cnt = contours
            .filter{Imgproc.contourArea(it) in 7500.0..35000.0}
            .filter{
                val rect = Imgproc.boundingRect(it)
                val ratio = rect.width.toDouble() / rect.height.toDouble()
                //println(ratio)
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
            //println("width : ${rect.width} / height : ${rect.height} / size : ${rect.height * rect.width}")
        }

        //cnt.forEach { it.release() }


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

            val maximiumAnalog = 960 // 조이스틱 최대값(절대)
            val maximiumRaw = 0.6 // slopeRaw의 최대값
            val percent = (slopeRaw / maximiumRaw * 100) * 0.6

            var analogSlope = maximiumAnalog * percent / 100 // 조이스틱에 들어가는 아날로그 값
            // 오차 방지를 위해 일정 범위값은 0으로 처리
            if(analogSlope in -30.0..30.0){ analogSlope = 0.0 }

            // 승강타 구하기
            val handAverage = (leftHand.size().area() + rightHand.size().area()) / 2
            val rawHandPositionPercent = (handAverage / 26000.0) * 100
            var elev = 1080 - ((1080/100) * rawHandPositionPercent)
            if(elev < 0) elev = 0.0

            //println("Position : ${rawHandPositionPercent}% / ${elev}px")

            val curPos = Point(960 + analogSlope, Math.pow(((elev.toInt() / 2) + 150).toDouble(), 1.15) - 250)
            //if(lastPosition == null || (Math.abs(lastPosition.x - curPos.x) in 0.0..220.0 && Math.abs(lastPosition.y - curPos.y) in 0.0..220.0) || ignoreCount >= 4){
                if(!debugMode && using){
                    controller.mouseMove(curPos.x.toInt(), curPos.y.toInt())
                }

                lastPosition = curPos
                ignoreCount = 0
            //}
            //ignoreCount++

            Imgproc.putText(foundForShow, "Slope : ${analogSlope}", Point(0.0, 30.0), Core.FONT_HERSHEY_COMPLEX, 1.0, Scalar(128.0, 0.0, 0.0))
        }else if(hands.size == 1 && Imgproc.boundingRect(hands[0]).width > Imgproc.boundingRect(hands[0]).height){
            usingSwitchTime += System.currentTimeMillis().toInt() - lastTime
            lastedChange = System.currentTimeMillis().toInt()
            println("Switching ${usingSwitchTime}ms(add ${System.currentTimeMillis().toInt() - lastTime}ms)")
        }

        // 1초 동안 손을 합치면
        if(usingSwitchTime >= 1000){
            using = !using
            if(using == false) controller.mouseMove(Toolkit.getDefaultToolkit().screenSize.width / 2, Toolkit.getDefaultToolkit().screenSize.height / 2)
            usingSwitchTime = 0
        }
        lastTime = System.currentTimeMillis().toInt()

        // 0.6초 이상 손 합치는 동작이 중단된다면
        if(usingSwitchTime > 0 && System.currentTimeMillis().toInt() - lastedChange > 600){
            println("================== !Clear! ==================")
            lastedChange = System.currentTimeMillis().toInt()
            usingSwitchTime = 0
        }

        rawHands.forEach { it.release() }

        // 결과물 출력
        if(debugMode){
            gui_Process.showImage(foundForProcess)
            gui_Show.showImage(foundForShow)
        }

        hand.release()
        foundForProcess.release()
        foundForShow.release()
        roiImage.release()
    }

}
