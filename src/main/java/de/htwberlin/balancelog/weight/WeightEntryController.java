package de.htwberlin.balancelog.weight;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/weight-entries")
public class WeightEntryController {

	private final WeightEntryService service;

	public WeightEntryController(WeightEntryService service) {
		this.service = service;
	}

	@GetMapping
	public List<WeightEntry> findAll() {
		return service.findAll();
	}

}
