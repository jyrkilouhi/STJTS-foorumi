
package tikape.runko.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import tikape.runko.domain.Viesti;


public class ViestiDao implements Dao<Viesti, Integer>{
    Database database;

    public ViestiDao(Database database) {
        this.database = database;
    }
    
    // luodaan tietokantaan uusi viesti
    public Viesti create(Viesti uusiViesti) throws SQLException {
        Connection connection = database.getConnection();
        PreparedStatement stmt = connection.prepareStatement("INSERT INTO Viesti (aihe_id, teksti, nimimerkki) "
                + "VALUES ( ? , ? , ? )");
        stmt.setObject(1, uusiViesti.getAihe_id());
        stmt.setObject(2, uusiViesti.getTeksti());
        stmt.setObject(3, uusiViesti.getNimimerkki());        
        stmt.execute();
        
        // haetaan uuden viestin viesti_id
        stmt = connection.prepareStatement("SELECT viesti_id, ajankohta FROM Viesti "
                + "WHERE aihe_id = ? AND nimimerkki = ? "
                + "ORDER BY ajankohta DESC;");       
        stmt.setObject(1, uusiViesti.getAihe_id());
        stmt.setObject(2, uusiViesti.getNimimerkki());
        ResultSet rs = stmt.executeQuery();
        
        // luonti epäonnistui, palautetaan null
        boolean hasOne = rs.next();
        if (!hasOne) {
            return null;
        }
        
        // palauetaan luodun viestin viesti_id
        int id = rs.getInt("viesti_id");
        String ajankohta = rs.getString("ajankohta");
        Viesti luotuViesti = new Viesti(id, uusiViesti.getAihe_id(), uusiViesti.getTeksti(), ajankohta , uusiViesti.getNimimerkki());
        stmt.close();
        connection.close();        
        return luotuViesti;
    }

    // ei tarvetta projektissa, ei tehty vaiko TODO
    @Override
    public Viesti findOne(Integer key) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    // ei tarvetta projektissa , ei tehty vaiko TODO
    @Override
    public List<Viesti> findAll() throws SQLException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    // etsitään yhden aiheen kaikki viestit aihe_id:n perusteella
    @Override
    public List<Viesti> findAllIn(Integer id) throws SQLException {

        Connection connection = database.getConnection();
        PreparedStatement stmt = connection.prepareStatement("SELECT * FROM Viesti "
                + "WHERE Viesti.aihe_id = ? ORDER BY Viesti.ajankohta;");       
        stmt.setObject(1, id);
        ResultSet rs = stmt.executeQuery();
        List<Viesti> viestit = new ArrayList<>();
        while (rs.next()) {
            int viesti_id = rs.getInt("viesti_id");
            String teksti = rs.getString("teksti");
            String nimimerkki = rs.getString("nimimerkki");
            String ajankohta = rs.getString("ajankohta");

            viestit.add(new Viesti(viesti_id, id, teksti, ajankohta, nimimerkki));
        }

        rs.close();
        stmt.close();
        connection.close();

        return viestit;        
    }    

    // ei tehty koska ei tarvetta, vaiko TODO
    @Override
    public void delete(Integer key) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

            
}
