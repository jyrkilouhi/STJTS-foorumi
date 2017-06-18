package tikape.runko;

import java.util.*;
import spark.ModelAndView;
import static spark.Spark.*;
import spark.template.thymeleaf.ThymeleafTemplateEngine;
import tikape.runko.database.*;
import tikape.runko.domain.*;
import tikape.runko.view.*;

public class Main {

    public static void main(String[] args) throws Exception {
        if (System.getenv("PORT") != null) {
            port(Integer.valueOf(System.getenv("PORT")));
        }
        // käytetään oletuksena paikallista sqlite-tietokantaa
        String jdbcOsoite = "jdbc:sqlite:tsjtsfoorumi.db";
        // jos heroku antaa käyttöömme tietokantaosoitteen, otetaan se käyttöön
        if (System.getenv("DATABASE_URL") != null) {
            jdbcOsoite = System.getenv("DATABASE_URL");
        } 

        Database database = new Database(jdbcOsoite);
        database.init();

        // Tietokanta rajapinnat Alue, Aihe ja Viesti luokille
        AlueDao alueDao = new AlueDao(database);
        AiheDao aiheDao = new AiheDao(database);
        ViestiDao viestiDao = new ViestiDao(database);

        // ADDED tiistaina 13.06.2017 klo 20:00
        // ohjataan pyynnöt "/index" sekä "/index.html" osoitteseen "/"
        get("/index", (req, res) -> {
            res.redirect("/");
            return "";
        });         
        get("/index.html", (req, res) -> {
            res.redirect("/");
            return "";
        });          
        
        // Käyttäjä avaa pääsivun-> näytetään kaikki alueet
        get("/", (req, res) -> {
            HashMap map = new HashMap<>(); 
            map.put("alueet", alueDao.findAll());
            return new ModelAndView(map, "alueet");
        }, new ThymeleafTemplateEngine());
                 
        // Lisätään uusi alue ja palataan pääsivulle. Jos saman niminen alue on jo olemassa ohjataan virhe sivulle.
        post("/alue", (req, res) -> {
            String alueSelite = req.queryParams("alue").trim();           
            if (!alueSelite.isEmpty()&&alueSelite.length()<51) {
                for(Alue alue : alueDao.findAll()) {
                    if(alue.getKuvaus().equals(alueSelite)) {
                        res.redirect("/virhe/alue/1");
                        return "";    
                    }
                }
                Alue uusiAlue = new Alue(alueSelite);
                alueDao.create(uusiAlue);
            }
            res.redirect("/");
            return "";
        });

        // alueen ":id" valittu, ohjataan sivulle /alueet/:id/sivu/1
        get("/alueet/:id", (req, res) -> {
            res.redirect("/alueet/" + req.params("id") + "/sivu/1");
            return "";
        });     
        
        // näytetään alueen ":id" avaukset sivu ":s"
        get("/alueet/:id/sivu/:s", (req, res) -> {
            HashMap map = new HashMap<>();
            Alue alue;
            int alue_id = 0;
            int haluttuSivu;
            
            try {
                alue_id = Integer.parseInt(req.params("id"));
                alue = alueDao.findOne(alue_id);
            } catch (NumberFormatException e) {
                alue = null;
            }
            
            if(alue == null) {
                res.redirect("/virhe/aluevalinta/" + req.params("id"));
                return new ModelAndView(map, "virhe");
            } else {
                try {
                    haluttuSivu = Integer.parseInt(req.params("s"));
                    if (haluttuSivu < 1) {
                        throw new NumberFormatException();
                    }
                } catch (NumberFormatException e) {
                    res.redirect("/alueet/" + alue_id + "/sivu/1");
                    return new ModelAndView(map, "virhe");
                }
                
                map.put("alue", alue);
                ArrayList<Aihe> kaikkiAiheet = (ArrayList) aiheDao.findAllIn(alue_id);
                Sivu sivut = new Sivu(kaikkiAiheet.size(), haluttuSivu, "location.href='/alueet/" + alue.getAlue_id() + "/sivu/", "'");
                map.put("aiheet", kaikkiAiheet.subList(sivut.getEkaRivi(), sivut.getVikaRivi()+1));
                map.put("sivut", sivut);
                return new ModelAndView(map, "aiheet");
            }
        }, new ThymeleafTemplateEngine());
        
        // Lisätään uusi aihe alueeseen "id". luodaan aiheeseen myös ensimmäinen viesti
        post("/aihe/:alue_id", (req, res) -> {
            String viesti = req.queryParams("viesti").trim();
            String nimimerkki = req.queryParams("nimimerkki").trim();
            String otsikko = req.queryParams("otsikko").trim();
            int alue_id;
            
            try {
                alue_id = Integer.parseInt(req.params("alue_id"));
            } catch (NumberFormatException e) {
                // Oletus että kelvollinen alue_id > 0 ?
                alue_id = -1;
            }
            
            if (alue_id > 0
                    && !viesti.isEmpty()
                    && !nimimerkki.isEmpty()
                    && !otsikko.isEmpty()
                    && viesti.length() < 501
                    && nimimerkki.length() < 26
                    && otsikko.length() < 51
                    && alueDao.findOne(alue_id) != null) {
                Aihe uusiAihe = new Aihe(otsikko, alue_id);
                Aihe luotuAihe = aiheDao.create(uusiAihe); 
                Viesti uusiViesti = new Viesti(luotuAihe.getAihe_id(), viesti, nimimerkki);
                viestiDao.create(uusiViesti);
            } else {
                res.redirect("/virhe/aihe/"+alue_id);
                return "";
            }
            // uusi aihe tulee listalla ensimmäiseksi joten siirrytään ko alueen listan alkuun.
            res.redirect("/alueet/"+alue_id);
            return "";
        });
        
        // aihe ":id" valittu, ohjataan sivulle /aiheet/:id/sivu/1
        get("/aiheet/:id", (req, res) -> {
            res.redirect("/aiheet/" + req.params("id") + "/sivu/1");
            return "";
        });     
        
        // näytetään aiheen ":id" viestit sivulta ":s"
        get("/aiheet/:id/sivu/:s", (req, res) -> {
            HashMap map = new HashMap<>();
            Aihe aihe;
            int aihe_id = 0;
            int haluttuSivu;
            
            try {
                aihe_id = Integer.parseInt(req.params("id"));
                aihe = aiheDao.findOne(aihe_id);
            } catch (NumberFormatException e) {
                aihe = null;
            }
            
            if(aihe == null) {
                res.redirect("/virhe/aihevalinta/" + req.params("id"));
                return new ModelAndView(map, "virhe");
            } else {
                try {
                    haluttuSivu = Integer.parseInt(req.params("s"));
                    if (haluttuSivu < 1) {
                        throw new NumberFormatException();
                    }
                } catch (NumberFormatException e) {
                    res.redirect("/aiheet/" + aihe_id + "/sivu/1");
                    return new ModelAndView(map, "virhe");
                }
                
                map.put("alue", alueDao.findOne(aihe.getAlue_id()));
                map.put("aihe", aihe); 
                ArrayList<Aihe> kaikkiViestit = (ArrayList) viestiDao.findAllIn(aihe.getAihe_id());
                Sivu sivut = new Sivu(kaikkiViestit.size(), haluttuSivu, "location.href='/aiheet/" + aihe.getAihe_id() + "/sivu/", "'");
                map.put("viestit", kaikkiViestit.subList(sivut.getEkaRivi(), sivut.getVikaRivi()+1));          
                map.put("sivut", sivut);
                return new ModelAndView(map, "viestit");
            }
        }, new ThymeleafTemplateEngine());
        
        // Lisätään uusi viesti ja palataan viestilista sivulle
        post("/viesti/:aihe_id", (req, res) -> {
            String viesti = req.queryParams("viesti").trim();
            String nimimerkki = req.queryParams("nimimerkki").trim();
            int aihe_id;
            
            try {
                aihe_id = Integer.parseInt(req.params("aihe_id"));
            } catch (NumberFormatException e) {
                aihe_id = -1;
            }
            
            if (aihe_id > 0
                    && !viesti.isEmpty()
                    && !nimimerkki.isEmpty()
                    && viesti.length() < 501
                    && nimimerkki.length() < 26
                    && aiheDao.findOne(aihe_id) != null) {
                Viesti uusiViesti = new Viesti(aihe_id, viesti, nimimerkki);
                viestiDao.create(uusiViesti);
            } else {
                res.redirect("/virhe/viesti/"+aihe_id);
                return "";              
            }
            int viimeinenSivu = (viestiDao.findAllIn(aihe_id).size()-1)/10+1;          
            res.redirect("/aiheet/"+aihe_id+"/sivu/"+viimeinenSivu);
            return "";
        });
        
        // Kutsuissa tapahtunut virhe, ohjataan siis virhesivulle
        get("/virhe/:viesti/:id", (req, res) -> {
            HashMap map = new HashMap<>();
            String viesti = req.params("viesti");
            if(viesti.equals("alue")) {
                map.put("virhekoodi", "Saman niminen alue on jo olemassa.");
                map.put("uusisivu", "/");
                map.put("sivunnimi", "Pääsivulle");
            }
            else if (viesti.equals("aluevalinta")) {
                map.put("virhekoodi", "Virheellinen aluevalinta. Aluetta " + req.params("id") + " ei ole tietokannassa.");
                map.put("uusisivu", "/");
                map.put("sivunnimi", "Pääsivulle");
            }
            else if(viesti.equals("aihe")) {
                map.put("virhekoodi", "Uuden keskustelun avauksen luonti epäonnistui.");
                map.put("uusisivu", "/alueet/"+req.params("id"));
                map.put("sivunnimi", "Takaisin aihe alueelle.");
            }
            else if (viesti.equals("aihevalinta")) {
                map.put("virhekoodi", "Virheellinen aihevalinta. Aihetta " + req.params("id") + " ei ole tietokannassa.");
                map.put("uusisivu", "/");
                map.put("sivunnimi", "Pääsivulle");
            }
            else if(viesti.equals("viesti")) {
                map.put("virhekoodi", "Uuden viestin luonti epäonnistui.");
                map.put("uusisivu", "/aiheet/"+req.params("id"));
                map.put("sivunnimi", "Takaisin viestiketjuun.");
            }                
            return new ModelAndView(map, "virhe");
        }, new ThymeleafTemplateEngine()); 

        TekstiUI.luoKayttoliittyma(alueDao, aiheDao, viestiDao);
    }
    
}
