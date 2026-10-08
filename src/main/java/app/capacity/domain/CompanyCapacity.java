package app.capacity.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class CompanyCapacity
{
    public static final double DEFAULT_DAILY_CAPACITY = 7.5;
    public static final long STANDARD_ID = 1L;

    @Id
    private Long id = STANDARD_ID;
    private double dailyCapacity = DEFAULT_DAILY_CAPACITY;

    public void update(double dailyCapacity)
    {
        this.dailyCapacity = dailyCapacity;
    }
}