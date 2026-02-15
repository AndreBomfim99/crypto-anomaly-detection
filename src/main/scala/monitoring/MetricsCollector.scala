package monitoring

import com.typesafe.scalalogging.LazyLogging
import java.io.{File, PrintWriter}
import java.time.{Instant, ZoneId}
import java.time.format.DateTimeFormatter
import scala.util.Try

case class PipelineMetrics(
  timestamp: Long,
  pipelineStage: String,
  cryptosProcessed: Int,
  recordsProcessed: Long,
  anomaliesDetected: Long,
  alertsSent: Int,
  executionTimeMs: Long,
  success: Boolean,
  errorMessage: Option[String] = None
)

case class SystemMetrics(
  timestamp: Long,
  memoryUsedMb: Long,
  memoryMaxMb: Long,
  cpuCores: Int
)

class MetricsCollector extends LazyLogging {
  
  private val metricsFile = new File("logs/metrics.log")
  private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    .withZone(ZoneId.of("UTC"))
  
  ensureMetricsFile()
  
  private def ensureMetricsFile(): Unit = {
    val logsDir = new File("logs")
    if (!logsDir.exists()) {
      logsDir.mkdirs()
      logger.info("Created logs directory")
    }
    
    if (!metricsFile.exists()) {
      metricsFile.createNewFile()
      writeHeader()
      logger.info(s"Created metrics file: ${metricsFile.getAbsolutePath}")
    }
  }
  
  private def writeHeader(): Unit = {
    val writer = new PrintWriter(metricsFile)
    try {
      writer.println("timestamp,pipeline_stage,cryptos_processed,records_processed," +
        "anomalies_detected,alerts_sent,execution_time_ms,success,error_message")
    } finally {
      writer.close()
    }
  }
  
  def recordPipelineMetrics(metrics: PipelineMetrics): Unit = {
    val timestampStr = formatter.format(Instant.ofEpochSecond(metrics.timestamp))
    val errorMsg = metrics.errorMessage.getOrElse("")
    
    val line = s"${timestampStr},${metrics.pipelineStage},${metrics.cryptosProcessed}," +
      s"${metrics.recordsProcessed},${metrics.anomaliesDetected},${metrics.alertsSent}," +
      s"${metrics.executionTimeMs},${metrics.success},${errorMsg}"
    
    appendToFile(line)
    
    logger.info(s"Pipeline metrics recorded: stage=${metrics.pipelineStage}, " +
      s"records=${metrics.recordsProcessed}, anomalies=${metrics.anomaliesDetected}, " +
      s"time=${metrics.executionTimeMs}ms")
  }
  
  def recordSystemMetrics(metrics: SystemMetrics): Unit = {
    logger.info(f"System metrics: Memory ${metrics.memoryUsedMb}%d/${metrics.memoryMaxMb}%d MB, " +
      f"CPU cores: ${metrics.cpuCores}%d")
  }
  
  def collectSystemMetrics(): SystemMetrics = {
    val runtime = Runtime.getRuntime
    val memoryUsed = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
    val memoryMax = runtime.maxMemory() / (1024 * 1024)
    val cpuCores = runtime.availableProcessors()
    
    SystemMetrics(
      timestamp = Instant.now().getEpochSecond,
      memoryUsedMb = memoryUsed,
      memoryMaxMb = memoryMax,
      cpuCores = cpuCores
    )
  }
  
  private def appendToFile(line: String): Unit = {
    val writer = new PrintWriter(new java.io.FileWriter(metricsFile, true))
    try {
      writer.println(line)
    } finally {
      writer.close()
    }
  }
  
  def generateReport(): String = {
    logger.info("Generating metrics report...")
    
    val lines = scala.io.Source.fromFile(metricsFile).getLines().toList
    
    if (lines.size <= 1) {
      return "No metrics available yet."
    }
    
    val dataLines = lines.tail
    val totalRuns = dataLines.size
    val successfulRuns = dataLines.count(_.split(",").last.trim == "true")
    val failedRuns = totalRuns - successfulRuns
    
    val totalRecords = dataLines.map { line =>
      val parts = line.split(",")
      if (parts.length >= 4) Try(parts(3).toLong).getOrElse(0L) else 0L
    }.sum
    
    val totalAnomalies = dataLines.map { line =>
      val parts = line.split(",")
      if (parts.length >= 5) Try(parts(4).toLong).getOrElse(0L) else 0L
    }.sum
    
    val report = s"""
       |═══════════════════════════════════════════════════════
       |METRICS REPORT
       |═══════════════════════════════════════════════════════
       |
       |Total Pipeline Runs:        $totalRuns
       |Successful Runs:             $successfulRuns
       |Failed Runs:                 $failedRuns
       |Success Rate:                ${if (totalRuns > 0) (successfulRuns.toDouble / totalRuns * 100).round else 0}%
       |
       |Total Records Processed:     $totalRecords
       |Total Anomalies Detected:    $totalAnomalies
       |Anomaly Rate:                ${if (totalRecords > 0) f"${(totalAnomalies.toDouble / totalRecords * 100)}%.2f" else "0.00"}%
       |
       |═══════════════════════════════════════════════════════
       """.stripMargin
    
    logger.info(report)
    report
  }
}

object MetricsCollector {
  def apply(): MetricsCollector = new MetricsCollector()
}
