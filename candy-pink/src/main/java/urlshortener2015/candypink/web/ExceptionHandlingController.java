package urlshortener2015.candypink.web;

import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import urlshortener2015.candypink.exceptions.ResourceNotFoundException;

@Controller
public class ExceptionHandlingController {

	@ExceptionHandler(ResourceNotFoundException.class)
	public ModelAndView handleError(HttpServletResponse res, ResourceNotFoundException exception) {
		ModelAndView mav = new ModelAndView();
		mav.addObject("exception", exception);
		mav.addObject("date", exception.getDate());
		// Redirection to notReachable
		mav.setViewName("notReachable");
		res.setStatus(HttpStatus.NOT_FOUND.value());
		return mav;
	}
}
