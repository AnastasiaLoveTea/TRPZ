package org.example.dlm.web;

import lombok.RequiredArgsConstructor;
import org.example.dlm.service.UserService;
import org.example.dlm.web.dto.RegisterForm;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @GetMapping("/login")
    public String loginPage() {
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("form", new RegisterForm());
        return "auth/register";
    }

    @PostMapping("/register")
    public String doRegister(@ModelAttribute("form") RegisterForm form, Model model) {
        String username = form.getUsername() != null ? form.getUsername().trim() : "";
        String password = form.getPassword() != null ? form.getPassword() : "";
        String confirm  = form.getConfirm()  != null ? form.getConfirm()  : "";

        form.setUsername(username);

        if (username.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
            model.addAttribute("error", "Логін і пароль не можуть бути порожніми");
            return "auth/register";
        }

        if (username.length() < 3 || username.length() > 20) {
            model.addAttribute("error", "Логін має бути від 3 до 20 символів");
            return "auth/register";
        }

        if (password.length() < 6) {
            model.addAttribute("error", "Пароль має містити 6 або більше символів");
            return "auth/register";
        }

        if (!password.equals(confirm)) {
            model.addAttribute("error", "Паролі мають збігатися");
            return "auth/register";
        }

        try {
            userService.register(username, password);
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            return "auth/register";
        }

        return "redirect:/login?registered";
    }
}
