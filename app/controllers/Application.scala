/*********************************************************
We're hiring! Check out http://www.42go.com/join_us.html
*********************************************************/


package controllers

import play.api._
import play.api.mvc.{Controller, Action}

import scala.slick.driver.H2Driver.simple._

import scala.slick.driver.BasicProfile
import scala.slick.lifted.{BaseTypeMapper, TypeMapperDelegate}
import scala.slick.session.{PositionedParameters,PositionedResult}

import play.api.libs.json.{Json, Format, JsValue, JsResult, JsSuccess, JsError, JsString, JsArray}
import play.api.Play.current
import play.api.db.DB




case class Email(addr: String)

object Email { 
  implicit val format = new Format[Email]{
    def reads(json: JsValue) : JsResult[Email] = {
      json match{
        case JsString(s) => JsSuccess(Email(s))
        case _ => JsError()
      }
    }

    def writes(email: Email) : JsValue = {
      JsString(email.addr)
    }
  }

  implicit object typeMapper extends BaseTypeMapper[Email] {
    def apply(profile: BasicProfile) : TypeMapperDelegate[Email] = {
      val delegate = profile.typeMapperDelegates.stringTypeMapperDelegate
      new TypeMapperDelegate[Email] {
        def sqlType = delegate.sqlType
        def setValue(value: Email, p: PositionedParameters) = delegate.setValue(value.addr, p)
        def setOption(valueOpt: Option[Email], p: PositionedParameters) = delegate.setOption(valueOpt.map(_.addr), p)
        def nextValue(r: PositionedResult): Email = Email(delegate.nextValue(r))
        def sqlTypeName = delegate.sqlTypeName
        def updateValue(value: Email, r: PositionedResult) = delegate.updateValue(value.addr, r)
        def zero = Email("towel@42go.com")
      }
    }
  }

}



case class Contact(name: String, phone: Option[Int], email: Option[Email])

object Contact {
  implicit val format = Json.format[Contact]
}



object Contacts extends Table[Contact]("CONTACTS") {
  def name = column[String]("NAME", O.PrimaryKey)
  def phone = column[Int]("PHONE", O.Nullable)
  def email = column[Email]("EMAIL", O.Nullable)
  def * = name ~ phone.? ~ email.? <> (Contact.apply _, Contact.unapply _)
}


object Application extends Controller {
  
  val database = Database.forDataSource(DB.getDataSource())


  def saveToDb(contact: Contact) = database.withSession{ implicit session: Session => 
    Contacts.*.insert(contact) 
  }

  def loadFromDb(name: String) = database.withSession{ implicit session: Session => 
    (for (row <- Contacts if row.name===name) yield row).list
  }


  def store(name: String) = Action(parse.tolerantJson) { request =>
    val phoneJson : JsValue = request.body \ "phone"
    val phone : Option[Int] = phoneJson.asOpt[Int]
    val email = (request.body \ "email").asOpt[Email]
    val contact = Contact(name, phone, email)
    saveToDb(contact)
    Ok("")
  }

  def lookup(name: String) = Action { request =>
    val contacts : Seq[Contact] = loadFromDb(name)
    val contactsJson : Seq[JsValue] = contacts.map(Json.toJson(_))
    Ok(JsArray(contactsJson))
  }

  
}
