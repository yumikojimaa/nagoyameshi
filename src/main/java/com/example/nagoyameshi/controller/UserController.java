package com.example.nagoyameshi.controller;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.nagoyameshi.entity.User;
import com.example.nagoyameshi.form.UserEditForm;
import com.example.nagoyameshi.repository.UserRepository;
import com.example.nagoyameshi.security.UserDetailsImpl;
import com.example.nagoyameshi.service.StripeService;
import com.example.nagoyameshi.service.UserService;
//import com.stripe.Stripe;
//import com.stripe.exception.StripeException;
//import com.stripe.model.Price;
//import com.stripe.model.PriceCollection;
//import com.stripe.model.checkout.Session;
//import com.stripe.param.checkout.SessionCreateParams;

import jakarta.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("/user")
public class UserController {
	@Value("${stripe.api-key}")
	private String stripeApiKey;
	private final UserRepository userRepository;
	private final UserService userService;
    private final StripeService stripeService;
	
	public UserController(UserRepository userRepository, 
			UserService userService,
			StripeService stripeService) {
		this.userRepository = userRepository;
		this.userService = userService;
		this.stripeService = stripeService;
	}
	
	@GetMapping
	public String index(@AuthenticationPrincipal UserDetailsImpl userDetailsImpl, Model model) {
		User user = userRepository.getReferenceById(userDetailsImpl.getUser().getId());
		model.addAttribute("user", user);
		var contract = stripeService.getContract(user);
		model.addAttribute("plan", (contract != null && !contract.isNormal()) ? "有料" : "無料" );
		model.addAttribute("canChangeCard",  (contract != null && !contract.isNormal()));
		return "user/index";
	}
	@GetMapping("/edit")
	public String edit(@AuthenticationPrincipal UserDetailsImpl userDetailsImpl, Model model) {
		User user = userRepository.getReferenceById(userDetailsImpl.getUser().getId());
		var contract = stripeService.getContract(user);
		UserEditForm userEditForm = new UserEditForm(user.getId(), user.getName(), user.getFurigana(), user.getEmail(),
				user.getPostalCode(), user.getAddress(), user.getPhoneNumber(), user.getBirthday(),
				user.getOccupation(), (contract != null));
		model.addAttribute("userEditForm", userEditForm);
		return "user/edit";
	}
	
	@PostMapping("/update")
     public String update(@ModelAttribute @Validated UserEditForm userEditForm, 
    		 BindingResult bindingResult, 
    		 RedirectAttributes redirectAttributes, 
    		 HttpServletRequest httpServletRequest
    		 ) {
         // メールアドレスが変更されており、かつ登録済みであれば、BindingResultオブジェクトにエラー内容を追加する
         if (userService.isEmailChanged(userEditForm) && userService.isEmailRegistered(userEditForm.getEmail())) {
             FieldError fieldError = new FieldError(bindingResult.getObjectName(), "email", "すでに登録済みのメールアドレスです。");
             bindingResult.addError(fieldError);                       
         }
         
         if (bindingResult.hasErrors()) {
             return "user/edit";
         }
         
         userService.update(userEditForm);
         redirectAttributes.addFlashAttribute("successMessage", "会員情報を編集しました。");
         
         if (userEditForm.getUseSubscription()) {
         
       	 var requestUrl = new String(httpServletRequest.getRequestURL());
     	 var success = requestUrl.replaceAll("/user/update", "/user");
     	 var cancel = requestUrl.replaceAll("/user/update", "/user/edit");
    	var session = stripeService.createSubscription(userEditForm.getEmail(), success, cancel);
        return String.format("redirect:%s", session.getUrl());
         }
         stripeService.deleteSubscription(userEditForm.getEmail());

         
         return "redirect:/user";
	 }
	
		@GetMapping("/change_card")
		public String changeCard(
				@AuthenticationPrincipal UserDetailsImpl userDetailsImpl, 
				Model model,
	   		 	HttpServletRequest httpServletRequest) {
			User user = userRepository.getReferenceById(userDetailsImpl.getUser().getId());
			var contract = stripeService.getContract(user);
			var requestUrl = new String(httpServletRequest.getRequestURL());
	   	 	var success = requestUrl.replaceAll("/user/change_card", "/user");
	   	 	var cancel = requestUrl.replaceAll("/user/change_card", "/user");
	  	    var session = stripeService.changeSubscriptionCard(contract, success, cancel);
	        return String.format("redirect:%s", session.getUrl());
		}
  
}