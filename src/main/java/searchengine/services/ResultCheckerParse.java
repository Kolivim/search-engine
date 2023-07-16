package searchengine.services;

import lombok.extern.slf4j.Slf4j;

import java.util.Iterator;
import java.util.Stack;
import java.util.concurrent.RunnableFuture;

@Slf4j
public class ResultCheckerParse implements Runnable {
    private final Stack<RunnableFuture<Boolean>> runnableFutureList;
    private IndexingService indexingService;
    private boolean isIndexingStopped;

    public boolean isIndexingStopped() {
        return isIndexingStopped;
    }

    public void setIndexingStopped(boolean indexingStopped) {
        this.isIndexingStopped = indexingStopped;
    }

    public ResultCheckerParse(Stack<RunnableFuture<Boolean>> runnableFutureList) {
        this.indexingService = (IndexingService) SpringUtils.ctx.getBean(IndexingService.class);
        this.runnableFutureList = runnableFutureList;
    }

    @Override
    public void run() {
        while (runnableFutureList.size() != 0) {
            for (Iterator<RunnableFuture<Boolean>> futureIterator = runnableFutureList.iterator();
                 futureIterator.hasNext(); ) {
                RunnableFuture<Boolean> future = futureIterator.next();
                if (future.isDone()) {
                    futureIterator.remove();
                }

                if (isIndexingStopped) {
                    boolean cancelled = future.cancel(true);
                    futureIterator.remove();
                    log.info("В методе run() - Индексация остановлена пользователем: {}, future.isCancelled: {}",
                            cancelled, future.isCancelled());
                }
            }
        }

        if (!isIndexingStopped) {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                log.error("В методе run() в теле if(!isIndexingStopped) - сработал InterruptedException: {}",
                        e.getMessage());
            }

            indexingService.setIndexingStarted(false);

            log.debug("Выполнена проверка if(!isIndexingStopped) после завершения цикла итерации future " +
                            "метода run() класса ResultCheckerParse, runnableFutureList.size() = {}" +
                            "\nЗначение локальной переменной класса RCP isIndexingStopped: {}" +
                            "\nЗначение флажковой переменной класса isIndexingStarted: {}",
                    runnableFutureList.size(), isIndexingStopped, indexingService.getIndexingStarted());
        }

        log.info("Завершение метода run() - runnableFutureList.size() = {} - значение переменной isIndexingStarted: {}",
                runnableFutureList.size(), indexingService.getIndexingStarted());
    }
}
