package net.stbbs.yasushi

import java.io.File
import org.slf4j.LoggerFactory
import org.fusesource.scalate.{TemplateSource, Binding, TemplateEngine}
import org.fusesource.scalate.servlet.ServletRenderContext
import org.fusesource.scalate.support.FileResourceLoader
import org.fusesource.scalate.util.IOUtil

object ScalatePrecompiler {
  val logger = LoggerFactory.getLogger("ScalateCli")
  def main(args: Array[String]) {
    assert(args.size >= 2, "invalid argument")

    val output = new File(args.head)
    val sources = args.drop(1).map(new File(_))

    logger.debug("outputDirectory: {}", output)
    logger.debug("sourceDirectories: {}", sources)

    output.mkdirs()

    // TODO need to customize bindings
    var engine = new TemplateEngine
    engine.bindings = createBindings()

    engine.resourceLoader = new FileResourceLoader(None)

    val paths = sources.flatMap(sd => collect(sd, "", engine.codeGenerators.keySet))

    logger.info("Precompiling Scalate Templates into Scala soruces...")

    for ((uri, file) <- paths) {
      logger.info("    processing {} (uri: {})", file, uri)

      val code = engine.generateScala(TemplateSource.fromFile(file, uri), createBindingsForPath(uri))
      val sourceFile = new File(output, uri.replace(':', '_') + ".scala")
      sourceFile.getParentFile.mkdirs
      IOUtil.writeBinaryFile(sourceFile, code.source.getBytes("UTF-8"))
    }
  }

  def collect(basedir: File, baseuri: String, exts: collection.Set[String]): Map[String, File] = {
    // TODO check same uri WEB-INF and other directory.
    def uri(d: File) = baseuri + (if (d.getName != "WEB-INF") "/" + d.getName else "")
    if (basedir.isDirectory) {
      val files = basedir.listFiles.toList
      val dirs = files.filter(_.isDirectory)
      files.filter(f => exts(f.getName.split("\\.").last)).map(f => (baseuri + "/" + f.getName, f)).toMap ++ dirs.flatMap(d => collect(d, uri(d), exts))
    } else
      Map.empty
  }

  def createBindings(): List[Binding] =
    List(Binding("context", classOf[ServletRenderContext].getName, true, isImplicit = true))
  def createBindingsForPath(uri:String): List[Binding] = Nil

}
