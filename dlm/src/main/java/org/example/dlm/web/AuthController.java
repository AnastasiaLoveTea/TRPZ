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
        if (form.getUsername() == null || form.getUsername().isBlank()
                || form.getPassword() == null || form.getPassword().isBlank()
                || !form.getPassword().equals(form.getConfirm())) {
            model.addAttribute("error", "Перевірте логін і паролі (паролі мають збігатися)");
            return "auth/register";
        }
        try {
            userService.register(form.getUsername(), form.getPassword());
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            return "auth/register";
        }
        return "redirect:/login?registered";
    }
}
