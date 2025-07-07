// Spring Boot 애플리케이션 클래스 또는 별도의 초기화 클래스
import com.project.farming.domain.plant.entity.Plant;
import com.project.farming.domain.plant.repository.PlantRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;
import java.util.Optional;

@Configuration
public class DataInitializer {

    private static final String CUSTOM_PLANT_NAME = "사용자 정의 작물";

    @Bean
    public CommandLineRunner initData(PlantRepository plantRepository) {
        return args -> {
            Optional<Plant> customPlant = plantRepository.findByName(CUSTOM_PLANT_NAME);
            if (customPlant.isEmpty()) {
                Plant plant = Plant.builder()
                        .name(CUSTOM_PLANT_NAME)
                        .englishName("Custom Plant")
                        .species("N/A")
                        .season("N/A")
                        .createdAt(LocalDate.now())
                        .updatedAt(LocalDate.now())
                        .build();
                plantRepository.save(plant);
                System.out.println("'" + CUSTOM_PLANT_NAME + "' Plant 엔트리가 성공적으로 추가되었습니다.");
            } else {
                System.out.println("'" + CUSTOM_PLANT_NAME + "' Plant 엔트리가 이미 존재합니다.");
            }
        };
    }
}