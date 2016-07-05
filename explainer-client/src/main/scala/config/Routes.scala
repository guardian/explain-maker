package config

object Routes {

  object ExplainEditor {
    val base = "/explain"
    def update(id: String) = base + s"/update/$id"
    def loadExplainer(id: String) = base + s"/api/$id"
  }
}
