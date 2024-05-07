package jigsaw.packagemodels;

import jigsaw.server.ormmodels.GameStatModel;

import java.io.Serializable;
import java.util.List;

/**
 * Данные для отправки через сокет.
 * Момент: перед началом новой игры клиент может запросить у сервера ТОП 10 игр.
 */
public class TopGamesPackage extends GeneralPackage implements Serializable {
    public List<GameStatModel> topGames;

    public TopGamesPackage(List<GameStatModel> topGames) {
        type = "top";
        this.topGames = topGames;
    }
}
