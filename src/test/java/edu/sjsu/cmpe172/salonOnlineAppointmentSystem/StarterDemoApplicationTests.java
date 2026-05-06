package edu.sjsu.cmpe172.salonOnlineAppointmentSystem;

import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.controller.HomeController;
import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(HomeController.class)
class StarterDemoApplicationTests {
    @MockitoBean
    private UserRepository userRepository;

    @Autowired
    private MockMvc mockMvc;

	@Test
	void homePageLoads() throws Exception {
		mockMvc.perform(get("/"))
				.andExpect(status().isOk())
				.andExpect(view().name("index"));
	}

}
