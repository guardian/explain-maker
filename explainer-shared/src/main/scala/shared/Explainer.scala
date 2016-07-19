package shared

case class ExplainerUpdate(field: String, value: String)

case class ExplainerFacet(
               title             : String,
               body              : String,
               last_modified_time: Long)

case class ExplainerItem(
              id   : String,
              draft: ExplainerFacet,
              live : Option[ExplainerFacet])

