package org.mycompany.controller;

import java.io.FileWriter;
import java.util.List;
import java.util.Scanner;

import javax.jms.ConnectionFactory;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.jms.JmsComponent;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.json.simple.JsonObject;
import org.mycompany.model.Candidat;
import org.mycompany.model.Entreprise;
import org.mycompany.model.Test;
import org.mycompany.repo.ICandidatRepository;
import org.mycompany.repo.IEntrepriseRepository;
import org.mycompany.repo.ITestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {
	private int count = 0;
	private static String url = "tcp://194.206.91.85:61616";
	Scanner scan = new Scanner(System.in);

	@Autowired
	ITestRepository itr;

	@Autowired
	ProducerTemplate producerTemplate;
	@Autowired
	EntrepriseController eco;
	@Autowired
	CandidatController cco;
	@Autowired
	IEntrepriseRepository ier;
	@Autowired
	ICandidatRepository icr;

	@GetMapping("/getTest/{id}")
	public Test getTest(@PathVariable int id) {
		return itr.findById(id).get();
	}

	@GetMapping("/getTests")
	public List<Test> getTests() {
		return itr.findAll();
	}

	@PostMapping("/saveTest")
	public void saveTest(@RequestBody Test pro) {
		itr.save(pro);
	}

	@DeleteMapping("/deleteTest/{id}")
	public void deleteTest(@PathVariable int id) {
		itr.deleteById(id);
	}

	@PutMapping("/updateTest{id}")
	public Test updateTest(@RequestBody Test newTest, @PathVariable int id) {
		return itr.findById(id).map(Test -> {
			Test.setId(newTest.getId());
			Test.setSujet(newTest.getSujet());
			Test.setValide(newTest.getValide());
			Test.setCandidat(newTest.getCandidat());
			Test.setEntreprise(newTest.getEntreprise());
			return itr.save(Test);
		}).orElseGet(() -> {
			return itr.save(newTest);
		});
	}

	@GetMapping("/lancerRouteTest")
	public void lanceRoute() throws Exception {
		CamelContext context = new DefaultCamelContext();
		ConnectionFactory connectionFactory = new ActiveMQConnectionFactory(url);
		connectionFactory.createConnection("admin", "adaming2022");
		context.addComponent("jms", JmsComponent.jmsComponentAutoAcknowledge(connectionFactory));
		context.start();
		producerTemplate.sendBody("direct:startTest", null);
		context.stop();
	}

	@GetMapping("/TestToJSON")
	public void TestToJSONFile(@RequestBody Test test) {

		JsonObject TestJSON = testToJson(test);
		String adresse = "inputTest/envoiTest" + count + ".json";

		try (FileWriter file = new FileWriter(adresse)) {
			String output = TestJSON.toJson().toString();
			file.write(output);
			file.flush();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public String testToJsonString(Test test) {
		JsonObject tJSON = new JsonObject();
		tJSON.put("id", test.getId());
		tJSON.put("sujet", test.getSujet());
		tJSON.put("valide", test.getValide());
		tJSON.put("candidat", test.getCandidat());
		tJSON.put("entreprise", test.getEntreprise());
		String output = tJSON.toJson().toString();
		return output;
	}

	public JsonObject testToJson(Test test) {
		JsonObject tJSON = new JsonObject();
		tJSON.put("id", test.getId());
		tJSON.put("sujet", test.getSujet());
		tJSON.put("valide", test.getValide());
		tJSON.put("candidat", null);
		tJSON.put("entreprise", null);
		return tJSON;
	}

	public Test promptTest() {
		List<Test> listeTest = this.getTests();
		int nouvelID = listeTest.size() + 1;

		System.out.println("Rentrez le sujet du test");
		String sujet = scan.nextLine();

		System.out.println("Quel est l'ID de l'entreprise ?");
		int idE = scan.nextInt();
		Entreprise ent = ier.findById(idE).get();

		System.out.println("Quel est l'ID du candidat ?");
		int idC = scan.nextInt();
		Candidat cand = icr.findById(idC).get();

		System.out.println("le candidat a il valide (boolean) ?");
		Boolean valid = scan.nextBoolean();

		Test te = new Test(nouvelID, sujet, valid, cand, ent);
		itr.save(te);
		return te;

	}
}
