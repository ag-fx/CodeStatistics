package com.airsaid.codestatistics.statistics

import com.airsaid.codestatistics.data.CodeType
import com.airsaid.codestatistics.data.StatisticsDetail
import javafx.application.Platform
import java.io.File
import java.util.concurrent.*

/**
 * @author airsaid
 */
class CodeStatistics(private var listener: CodeStatisticsListener? = null) {

  private val executor = ThreadPoolExecutor(corePoolSize, maxPoolSize,
      60, TimeUnit.SECONDS, LinkedBlockingQueue(workQueueSize),
      Executors.defaultThreadFactory(), ThreadPoolExecutor.CallerRunsPolicy())
  private val executorService = ExecutorCompletionService<StatisticsDetail?>(executor)

  private val dispatchThread = Executors.newSingleThreadExecutor()
  private val handlerThread = Executors.newSingleThreadExecutor()

  private lateinit var types: Map<String, CodeType>
  private val added = HashSet<String>()

  fun startStatistics(dirs: List<File>, types: Map<String, CodeType>) {
    this.added.clear()
    this.types = types

    listener?.beforeStatistics()

    // 由于任务的处理时间远比生产任务的耗时多，因此只使用一个线程派发任务
    dispatchThread.submit {
      dirs.forEach { recurScanFile(it) }
      // 任务添加完毕，最后添加一个 "毒丸" 对象用于判断是否结束
      executorService.submit { null }
    }

    // 开启一个线程处理已经执行完成的任务
    handlerThread.submit {
      while (true) {
        val statistics = executorService.take().get()
        if (statistics != null) {
          Platform.runLater { listener?.statistics(statistics) }
        } else {
          Platform.runLater { listener?.afterStatistics() }
          break
        }
      }
    }
  }

  fun stopStatistics() {
    executor.shutdownNow()
    dispatchThread.shutdownNow()
    handlerThread.shutdownNow()
  }

  fun setCodeStatisticsListener(listener: CodeStatisticsListener) {
    this.listener = listener
  }

  private fun recurScanFile(file: File) {
    if (dispatchThread.isShutdown ||
        !file.exists() || added.contains(file.path)) return

    if (file.isDirectory) { // 是目录则递归扫描
      val listFile = file.listFiles()
      if (listFile != null && listFile.isNotEmpty()) {
        listFile.forEach {
          if (!dispatchThread.isShutdown) recurScanFile(it)
        }
      }
    } else if (file.isFile && types.contains(file.extension)) {
      // 符合文件类型，将任务提交给线程池执行
      added.add(file.path)
      executorService.submit(StatisticsCallable(file, types[file.extension] as CodeType))
    }
  }

  companion object {
    val corePoolSize = Runtime.getRuntime().availableProcessors()
    val maxPoolSize = corePoolSize * (1 + (8 / 2))
    const val workQueueSize = 100
  }

}