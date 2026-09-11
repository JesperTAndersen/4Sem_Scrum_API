package app.dao;

import app.entities.Stage;
import jakarta.persistence.EntityManagerFactory;

public class StageDAO
    extends Crud<Stage>
{
    public StageDAO(EntityManagerFactory emf)
    {
        super(emf);
    }
}
