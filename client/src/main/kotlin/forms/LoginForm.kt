package forms

import org.w3c.dom.HTMLFormElement
import org.w3c.dom.HTMLInputElement
import react.FC
import react.Props
import react.dom.events.ChangeEventHandler
import react.dom.events.FormEventHandler
import react.dom.html.InputType
import react.dom.html.ReactHTML
import react.dom.html.ReactHTML.form
import react.dom.html.ReactHTML.input
import react.useState

external interface InputProps : Props {
    var onSubmit: (String, String) -> Unit
}

val InputComponent = FC<InputProps> { props ->
    val (login, setLogin) = useState("")
    val (password, setPassword) = useState("")

    val submitHandler: FormEventHandler<HTMLFormElement> = {
        it.preventDefault()
        setLogin("")
        props.onSubmit(login, password)
    }

    val loginChangeHandler: ChangeEventHandler<HTMLInputElement> = {
        setLogin(it.target.value)
    }

    val passwordChangeHandler: ChangeEventHandler<HTMLInputElement> = {
        setPassword(it.target.value)
    }

    form {
        onSubmit = submitHandler
        input {
            type = InputType.text
            onChange = loginChangeHandler
            value = login
            name = "Login"
            placeholder = "Login"
        }
        input {
            type = InputType.password
            onChange = passwordChangeHandler
            value = password
            name = "Login"
            placeholder = "Password"
        }
        ReactHTML.input {
            type = InputType.submit
            value = "Submit"
        }
    }
}
