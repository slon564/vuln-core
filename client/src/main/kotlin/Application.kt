import forms.InputComponent
import kotlinx.browser.document
import kotlinx.css.*
import kotlinx.html.js.onClickFunction
import react.*
import react.dom.render
import react.router.Route
import react.router.Routes
import react.router.dom.BrowserRouter
import styled.css
import styled.styledButton

fun main() {
    render(document.getElementById("react-app") ?: error("Couldn't find react-app")) {
        BrowserRouter {
            Routes {
                Route {
                    attrs.path = "/hello-world"
                    attrs.element = helloWorld.create()
                }
                Route {
                    attrs.path = "/login"
                    attrs.element = loginForm.create()
                }
                Route {
                    attrs.path = "/login/admin"
                    attrs.element = loginForm.create()
                }
            }
        }
    }
}

val loginForm = fc<Props> {
    InputComponent {
        attrs.onSubmit = submit
    }
}

val submit = { login: String, password: String -> console.log(login + password) }

val helloWorld = fc<Props> {
    var flag by useState(false)
    styledButton {
        css {
            color = if (flag) Color.red else Color.blue
        }
        attrs.onClickFunction = {
            flag = !flag
        }
        +"Hello world"
    }
}