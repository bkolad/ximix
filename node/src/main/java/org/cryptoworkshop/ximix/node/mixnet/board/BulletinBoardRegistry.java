/**
 * Copyright 2013 Crypto Workshop Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.cryptoworkshop.ximix.node.mixnet.board;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;

import org.cryptoworkshop.ximix.common.util.EventNotifier;
import org.cryptoworkshop.ximix.node.mixnet.transform.Transform;
import org.cryptoworkshop.ximix.node.service.CrossSection;
import org.cryptoworkshop.ximix.node.service.Decoupler;
import org.cryptoworkshop.ximix.node.service.NodeContext;

/**
 * Registry for bulletin boards.
 */
public class BulletinBoardRegistry
{
    private final NodeContext nodeContext;
    private final File workingDirectory;
    private final Map<String, Transform> transforms;
    private final Executor boardUpdateExecutor;
    private final CrossSection statistics;
    private final BulletinBoardChangeListener changeListener;

    private final Map<String, BulletinBoard> boards = new HashMap<>();
    private Map<String, BulletinBoard> transitBoards = new HashMap<String, BulletinBoard>();
    private Map<String, BulletinBoard> backupBoards = new HashMap<>();
    private Set<String> suspendedBoards = new HashSet<>();
    private Set<String> dowloadLockedBoards = new HashSet<>();
    private Set<String> shuffleLockedBoards = new HashSet<>();
    private Set<String> inTransitBoards = new HashSet<>();
    private Set<String> completedBoards = new HashSet<>();

    /**
     * Base constructor.
     *
     * @param nodeContext the context of the node we are associated with.
     * @param transforms a Map of transforms this node supports.
     * @param statistics the statistics sampling object to notify of changes.
     */
    public BulletinBoardRegistry(NodeContext nodeContext, Map<String, Transform> transforms, final CrossSection statistics)
    {
        this.nodeContext = nodeContext;
        this.transforms = transforms;
        this.boardUpdateExecutor = nodeContext.getDecoupler(Decoupler.BOARD_REGISTRY);
        this.statistics = statistics;

        changeListener = new BulletinBoardChangeListener()
        {
            @Override
            public void messagesAdded(BulletinBoard bulletinBoard, int count)
            {
                statistics.increment("bhs!messages-on-board!" + bulletinBoard.getName(), count);
            }

            @Override
            public void messagesRemoved(BulletinBoardImpl bulletinBoard, int count)
            {
                statistics.decrement("bhs!messages-on-board!" + bulletinBoard.getName(), count);
            }
        };

        File homeDirectory = nodeContext.getHomeDirectory();

        if (homeDirectory != null)
        {
            this.workingDirectory = new File(homeDirectory, "boards");
            if (!this.workingDirectory.exists())
            {
                if (!workingDirectory.mkdir())
                {
                    nodeContext.getEventNotifier().notify(EventNotifier.Level.ERROR, "Unable to create registry working directory: " + workingDirectory.getPath());
                }
            }

            // initialise board state
            File[] files = workingDirectory.listFiles(new FilenameFilter()
            {
                @Override
                public boolean accept(File dir, String name)
                {
                    name = name.toLowerCase();

                    if (name.endsWith(".p") || name.endsWith(".t"))
                    {
                        return false;
                    }

                    if (name.contains("board.state"))
                    {
                        return false;
                    }

                    return true;
                }
            });

            for (int i = 0; i != files.length; i++)
            {
                File file = files[i];
                String name = file.getName();

                if (name.matches(".*\\.[0-9]+$"))
                {
                    transitBoards.put(file.getName(), new BulletinBoardImpl(name.substring(name.indexOf(".") + 1, name.lastIndexOf(".")), file, boardUpdateExecutor, nodeContext.getEventNotifier()));
                }
                else if (file.getName().endsWith(".backup"))
                {
                    backupBoards.put(name, new BulletinBoardImpl(name.substring(0, name.indexOf(".backup")), file, boardUpdateExecutor, nodeContext.getEventNotifier()));
                }
                else
                {
                    BulletinBoardImpl hostedBoard = new BulletinBoardImpl(name, file, boardUpdateExecutor, nodeContext.getEventNotifier());
                    boards.put(name, hostedBoard);

                    // re-instate the back up listener if needed.
                    if (hostedBoard.getBackupHost() != null)
                    {
                        hostedBoard.addListener(new BoardRemoteBackupListener(nodeContext, hostedBoard.getBackupHost()));
                    }
                }
            }
        }
        else
        {
            workingDirectory = null;
        }
    }

    public BulletinBoard createBoard(final String boardName)
    {
        synchronized (boards)
        {
            BulletinBoard board = boards.get(boardName);

            // TODO: need to detect twice!
            if (board == null)
            {
                statistics.addPlaceholderValue("bhs!messages-on-board!" + boardName, 0);

                File boardDBFile = deriveBoardFile(boardName);

                board = new BulletinBoardImpl(boardName, boardDBFile, nodeContext.getDecoupler(Decoupler.BOARD_LISTENER), nodeContext.getEventNotifier());
                board.addListener(changeListener);

                boards.put(boardName, board);
            }

            return board;
        }
    }

    public BulletinBoard createBoard(String boardName, String backUpHost)
    {
        BulletinBoard board = createBoard(boardName);

        board.addListener(new BoardRemoteBackupListener(nodeContext, backUpHost));

        return board;
    }

    /**
     * Returns a null board file if the workingDirectory is not specified.
     * It assumes that if no workingDirectory is specified there was no intention
     * to persist data.
     *
     * @param boardName
     * @return board working directory, null if none is available.
     */
    private File deriveBoardFile(String boardName)
    {
        if (workingDirectory != null)
        {
            return new File(workingDirectory, boardName);
        }

        return null;
    }

    public BulletinBoard getBoard(final String boardName)
    {
        synchronized (boards)
        {
            return boards.get(boardName);
        }
    }

    public String[] getBoardNames()
    {
        synchronized (boards)
        {
            return boards.keySet().toArray(new String[boards.size()]);
        }
    }

    public Transform[] getTransforms()
    {
        return transforms.values().toArray(new Transform[transforms.size()]);
    }

    public Set<String> getTransformNames()
    {
        Set<String> transformNames = new HashSet<String>();
        for (Transform transform : transforms.values())
        {
            transformNames.add(transform.getName());
        }

        return transformNames;
    }

    public Transform getTransform(String transformName)
    {
        return transforms.get(transformName).clone();
    }

    public boolean isSuspended(String boardName)
    {
        synchronized (boards)
        {
            return suspendedBoards.contains(boardName);
        }
    }

    public void activateBoard(String boardName)
    {
        synchronized (boards)
        {
            suspendedBoards.remove(boardName);
        }
    }

    public void suspendBoard(String boardName)
    {
        synchronized (boards)
        {
            suspendedBoards.add(boardName);
        }
    }

    public boolean isLocked(String boardName)
    {
        return isDownloadLocked(boardName) || isShuffleLocked(boardName) || isSuspended(boardName);
    }

    public boolean isDownloadLocked(String boardName)
    {
        synchronized (boards)
        {
            return dowloadLockedBoards.contains(boardName);
        }
    }

    public void downloadLock(String boardName)
    {
        synchronized (boards)
        {
            dowloadLockedBoards.add(boardName);
        }
    }

    public void downloadUnlock(String boardName)
    {
        synchronized (boards)
        {
            dowloadLockedBoards.remove(boardName);
        }
    }

    public boolean isShuffleLocked(String boardName)
    {
        synchronized (boards)
        {
            return shuffleLockedBoards.contains(boardName);
        }
    }

    public void shuffleLock(String boardName)
    {
        synchronized (boards)
        {
            shuffleLockedBoards.add(boardName);
        }
    }

    public void shuffleUnlock(String boardName)
    {
        synchronized (boards)
        {
            shuffleLockedBoards.remove(boardName);
        }
    }

    public boolean hasBoard(String boardName)
    {
        synchronized (boards)
        {
            return boards.containsKey(boardName);
        }
    }

    public BulletinBoard createBackupBoard(String boardName)
    {
        synchronized (boards)
        {
            BulletinBoard board = backupBoards.get(boardName);

            // TODO: need to detect twice!
            if (board == null)
            {
                board = new BulletinBoardImpl(boardName, deriveBoardFile(boardName + ".backup"), nodeContext.getDecoupler(Decoupler.BOARD_LISTENER), nodeContext.getEventNotifier());

                backupBoards.put(boardName, board);
            }

            return board;
        }
    }

    public BulletinBoard getBackupBoard(String boardName)
    {
        synchronized (boards)
        {
            BulletinBoard board = backupBoards.get(boardName);

            return board;
        }
    }

    public BulletinBoard getTransitBoard(long operationNumber, int stepNumber)
    {
        synchronized (boards)
        {
            List<String> transitBoardNames = getTransitBoardNames(operationNumber);
            String suffix = "." + stepNumber;

            for (String name : transitBoardNames)
            {
                if (name.endsWith(suffix))
                {
                    return transitBoards.get(name);
                }
            }

            throw new IllegalStateException("unable to find board for operation " + operationNumber);
        }
    }

    public BulletinBoard getTransitBoard(long operationNumber, String boardName, int stepNumber)
    {
        synchronized (boards)
        {
            String transitBoardName = getTransitBoardName(operationNumber, boardName, stepNumber);
            BulletinBoard board = transitBoards.get(transitBoardName);

            // TODO: need to detect twice!
            if (board == null)
            {
                board = new BulletinBoardImpl(boardName, deriveBoardFile(transitBoardName), nodeContext.getDecoupler(Decoupler.BOARD_LISTENER), nodeContext.getEventNotifier());

                transitBoards.put(transitBoardName, board);
            }

            return board;
        }
    }

    public void markInTransit(long operationNumber, String boardName, int stepNumber)
    {
        synchronized (boards)
        {
            String transitName = getTransitBoardName(operationNumber, boardName, stepNumber);

            inTransitBoards.add(transitName);
            completedBoards.remove(transitName);
        }
    }

    public void markCompleted(long operationNumber, String boardName, int stepNumber)
    {
        synchronized (boards)
        {
            String transitName = getTransitBoardName(operationNumber, boardName, stepNumber);

            completedBoards.add(transitName);
            inTransitBoards.remove(transitName);
        }
    }

    public boolean isInTransit(long operationNumber, String boardName, int stepNumber)
    {
        synchronized (boards)
        {
            String transitName = getTransitBoardName(operationNumber, boardName, stepNumber);

            return inTransitBoards.contains(transitName);
        }
    }

    public boolean isComplete(long operationNumber, String boardName, int stepNumber)
    {
        synchronized (boards)
        {
            String transitName = getTransitBoardName(operationNumber, boardName, stepNumber);

            return completedBoards.contains(transitName);
        }
    }

    private String getTransitBoardName(long operationNumber, String boardName, int stepNumber)
    {
        return operationNumber + "." + boardName + "." + stepNumber;
    }

    public List<String> getTransitBoardNames(long operationNumber)
    {
        String baseName = Long.toString(operationNumber);
        List<String> names = new ArrayList<>();

        for (String name : transitBoards.keySet())
        {
            if (name.startsWith(baseName))
            {
                names.add(name);
            }
        }

        return names;
    }
}
