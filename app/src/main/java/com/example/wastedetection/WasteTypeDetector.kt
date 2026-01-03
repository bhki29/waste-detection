package com.example.wastedetection

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.CastOp
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.util.PriorityQueue
import kotlin.math.max
import kotlin.math.min

class WasteTypeDetector(context: Context) {

    companion object {
        private const val MODEL_NAME = "model_prediksi_jenis_sampah.tflite"
        private const val INPUT_SIZE = 640
        // Ambang batas keyakinan (Naikkan agar noise hilang)
        private const val CONFIDENCE_THRESHOLD = 0.6f
        // Ambang batas tumpang tindih (Semakin kecil = semakin galak menghapus duplikat)
        private const val IOU_THRESHOLD = 0.45f
    }

    private var interpreter: Interpreter? = null
    val labels = listOf("Organik", "Anorganik")

    init {
        val options = Interpreter.Options()
        options.setNumThreads(4)
        try {
            val modelFile = FileUtil.loadMappedFile(context, MODEL_NAME)
            interpreter = Interpreter(modelFile, options)
        } catch (e: Exception) {
            Log.e("YOLO", "Error loading model", e)
        }
    }

    fun detect(bitmap: Bitmap): List<BoundingBox> {
        if (interpreter == null) return emptyList()

        val imageProcessor = ImageProcessor.Builder()
            .add(ResizeOp(INPUT_SIZE, INPUT_SIZE, ResizeOp.ResizeMethod.BILINEAR))
            .add(NormalizeOp(0f, 255f))
            .add(CastOp(DataType.FLOAT32))
            .build()

        var tensorImage = TensorImage(DataType.FLOAT32)
        tensorImage.load(bitmap)
        tensorImage = imageProcessor.process(tensorImage)

        // Setup Output
        val outputTensor = interpreter!!.getOutputTensor(0)
        val outputShape = outputTensor.shape() // Biasanya [1, 6, 8400]
        val outputBuffer = TensorBuffer.createFixedSize(outputShape, DataType.FLOAT32)

        interpreter?.run(tensorImage.buffer, outputBuffer.buffer.rewind())

        val rawOutput = outputBuffer.floatArray
        val boxes = ArrayList<BoundingBox>()

        // Asumsi Format Output YOLOv8 Default: [1, 6, 8400]
        // Dimana 6 = [x, y, w, h, score1, score2]
        // Dan 8400 = Jumlah kotak prediksi
        val channels = outputShape[1] // 6
        val anchors = outputShape[2]  // 8400

        for (i in 0 until anchors) {
            // Akses data secara berurutan sesuai bukti screenshot pertama
            val scoreOrganik = rawOutput[(4 * anchors) + i]
            val scoreAnorganik = rawOutput[(5 * anchors) + i]

            val maxScore = max(scoreOrganik, scoreAnorganik)
            val classIndex = if (scoreOrganik > scoreAnorganik) 0 else 1

            if (maxScore > CONFIDENCE_THRESHOLD) {
                val cx = rawOutput[(0 * anchors) + i]
                val cy = rawOutput[(1 * anchors) + i]
                val w = rawOutput[(2 * anchors) + i]
                val h = rawOutput[(3 * anchors) + i]

                // Logika Scaling Sederhana (Mengembalikan ke logika awal yang sukses)
                // Jika nilai > 1, berarti pixel -> bagi 640. Jika < 1, berarti normalized -> biarkan.
                val finalCx = cx / INPUT_SIZE
                val finalCy = cy / INPUT_SIZE
                val finalW = w / INPUT_SIZE
                val finalH = h / INPUT_SIZE

                val left = max(0f, finalCx - (finalW / 2))
                val top = max(0f, finalCy - (finalH / 2))
                val right = min(1f, finalCx + (finalW / 2))
                val bottom = min(1f, finalCy + (finalH / 2))

                boxes.add(
                    BoundingBox(
                        x1 = left, y1 = top, x2 = right, y2 = bottom,
                        cx = cx, cy = cy, w = w, h = h,
                        cnf = maxScore, cls = classIndex, label = labels[classIndex]
                    )
                )
            }
        }

        // Filter Duplikat (NMS)
        return nms(boxes)
    }

    // Algoritma NMS yang Lebih Stabil
    private fun nms(boxes: List<BoundingBox>): List<BoundingBox> {
        val sortedBoxes = boxes.sortedByDescending { it.cnf }.toMutableList()
        val selectedBoxes = ArrayList<BoundingBox>()

        while (sortedBoxes.isNotEmpty()) {
            val first = sortedBoxes[0]
            selectedBoxes.add(first)
            sortedBoxes.removeAt(0)

            val iterator = sortedBoxes.iterator()
            while (iterator.hasNext()) {
                val nextBox = iterator.next()
                val iou = calculateIoU(first, nextBox)
                // Jika tumpang tindih > 45%, hapus kotak yang skornya lebih kecil
                if (iou >= IOU_THRESHOLD) {
                    iterator.remove()
                }
            }
        }
        return selectedBoxes
    }

    private fun calculateIoU(box1: BoundingBox, box2: BoundingBox): Float {
        val xA = max(box1.x1, box2.x1)
        val yA = max(box1.y1, box2.y1)
        val xB = min(box1.x2, box2.x2)
        val yB = min(box1.y2, box2.y2)

        if (xB < xA || yB < yA) return 0f

        val intersection = (xB - xA) * (yB - yA)
        val box1Area = (box1.x2 - box1.x1) * (box1.y2 - box1.y1)
        val box2Area = (box2.x2 - box2.x1) * (box2.y2 - box2.y1)

        return intersection / (box1Area + box2Area - intersection)
    }

    data class BoundingBox(
        val x1: Float, val y1: Float, val x2: Float, val y2: Float,
        val cx: Float, val cy: Float, val w: Float, val h: Float,
        val cnf: Float, val cls: Int, val label: String
    )
}