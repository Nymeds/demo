package studdy.example.demo.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StartupBannerConfig {

    @Bean
    CommandLineRunner rafaelBanner() {
        return args -> System.out.println("""
                
                ===============================================================
                
                RRRR    AAAAA  FFFFF  AAAAA  EEEEE  L
                R   R   A   A  F      A   A  E      L
                RRRR    AAAAA  FFFF   AAAAA  EEEE   L
                R R     A   A  F      A   A  E      L
                R  RR   A   A  F      A   A  EEEEE  LLLLL
                
                EEEEE  M   M  U   U  IIIII  TTTTT   OOO
                E      MM MM  U   U    I      T    O   O
                EEEE   M M M  U   U    I      T    O   O
                E      M   M  U   U    I      T    O   O
                EEEEE  M   M   UUU   IIIII    T     OOO
                
                FFFFF   OOO   DDDD    A
                F      O   O  D   D  A A
                FFFF   O   O  D   D  AAAAA
                F      O   O  D   D  A   A
                F       OOO   DDDD   A   A
                
                     
                
                ===============================================================
                """);
    }
}