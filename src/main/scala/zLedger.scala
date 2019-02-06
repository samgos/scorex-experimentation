
object zLedger extends App {

  val filename = args.headOption.getOrElse("settings.json")
  var application = new zInit(filename)

  application.run()

}
