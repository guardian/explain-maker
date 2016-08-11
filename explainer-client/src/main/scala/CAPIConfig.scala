
case class TagObject(id: String, webTitle: String)
case class CAPIResponseTagsSearchResponseObject(results: List[TagObject])
case class CAPIResponseTagsSearch(response: CAPIResponseTagsSearchResponseObject)

object CAPIConfig {
  val capiTagQueryURL = "https://content.guardianapis.com/tags"
}