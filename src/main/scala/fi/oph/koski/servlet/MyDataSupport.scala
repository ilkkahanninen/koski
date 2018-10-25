package fi.oph.koski.servlet

import java.net.URLEncoder

import fi.oph.koski.http.KoskiErrorCategory
import org.scalatra.ScalatraServlet
import com.typesafe.config.{Config => TypeSafeConfig}
import fi.oph.koski.mydata.MyDataConfig


trait MyDataSupport extends ScalatraServlet with MyDataConfig {
  override def hasConfigForMember(id: String = memberCodeParam): Boolean
  override def getConfigForMember(id: String = memberCodeParam): TypeSafeConfig
  def mydataLoginServletURL: String = conf.getString("login.servlet")

  def urlEncode(str: String): String = URLEncoder.encode(str, "UTF-8")

  def getLoginURL(target: String = getCurrentURL, encode: Boolean = false): String = {
    if (encode) {
      urlEncode(s"${mydataLoginServletURL}?onSuccess=${urlEncode(target)}")
    } else {
      s"${mydataLoginServletURL}?onSuccess=${target}"
    }
  }

  def getShibbolethLoginURL(target: String = getCurrentURL, lang: String) = {
    conf.getString(s"login.shibboleth.$lang") +
      conf.getString("login.shibboleth.targetparam") + getLoginURL(target, encode = true) +
      getKorhopankkiRedirectURLParameter(target)
  }

  def getKorhopankkiRedirectURLParameter(target: String): String = {
    if(application.config.getString("shibboleth.security") == "mock") {
      s"&redirect=${urlEncode(target)}"
    } else {
      ""
    }
  }

  def getCurrentURL: String = {
    if (request.queryString.isEmpty) {
      request.getRequestURI
    } else {
      request.getRequestURI + s"?${request.queryString}"
    }
  }

  def memberCodeParam: String = {
    if (params("memberCode") == null) {
      throw InvalidRequestException(KoskiErrorCategory.badRequest.queryParam.missing("Vaadittu valtuutuksen kumppani-parametri puuttuu"))
    }

    params("memberCode")
  }
}

