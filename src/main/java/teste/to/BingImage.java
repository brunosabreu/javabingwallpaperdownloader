package teste.to;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * TO com as informações de uma imagem.
 */
@NoArgsConstructor
@Data
public class BingImage {
    private String startdate;
    private String enddate;
    private String copyright;
    private String copyrightlink;
    private String urlbase;
    private String fullstartdate;
    private String url;
    private String title;
    private String quiz;
    private String wp;
    private String hsh;

    /*
    Exemplo:
        "startdate": "20210411",
        "fullstartdate": "202104112300",
        "enddate": "20210412",
        "url": "/th?id=OHR.YurisNight_EN-GB3099076529_1920x1080.jpg&rf=LaDigue_1920x1080.jpg&pid=hp",
        "urlbase": "/th?id=OHR.YurisNight_EN-GB3099076529",
        "copyright": "Earth from the International Space Station, photographed by astronaut Jeff Williams (© Jeff Williams/Nasa)",
        "copyrightlink": "https://www.bing.com/search?q=space+exploration+history&FORM=hpcapt&filters=HpDate%3a%2220210411_2300%22",
        "title": "In orbit for Yuri's Night",
        "quiz": "/search?q=Bing+homepage+quiz&filters=WQOskey:%22HPQuiz_20210411_YurisNight%22&FORM=HPQUIZ",
        "wp": true,
        "hsh": "39f813e397bb2b5684ea9d79bd3c217b",
        "drk": 1,
        "top": 1,
        "bot": 1,
        "hs": []
    */
}
