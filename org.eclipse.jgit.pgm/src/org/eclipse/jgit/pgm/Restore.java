package org.eclipse.jgit.pgm;


import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.RestoreCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;

import java.util.ArrayList;
import java.util.List;

@Command(common = true, usage = "usage_addFileContentsToTheIndex")
class Restore extends TextBuiltin{

//    @Option(name = "--staged", usage = "usage_addRenormalize")
//    private boolean toIndex = false;
//
//    @Option(name = "--worktree", aliases = {"-w"},usage = "usage_addRenormalize")
//    private boolean toWorkTree = false;
//
//    @Option(name="--source", aliases = { "-s" }, usage="specifies the name")
//    private String source;
//
//    @Argument(required = true, metaVar = "metaVar_filepattern", usage = "usage_filesToAddContentFrom")
//    private List<String> filepatterns = new ArrayList<>();
//
//
//    @Override
//    protected void run() throws Exception {
//        try (Git git = new Git(db)){
//            RestoreCommand cmd = git.restore();
//
//            cmd.setUpdateIndex(toIndex).setUpdateWorkTree(toWorkTree).setSource(source);
//
//            for (String s:filepatterns){
//                cmd.addFilepattern(s);
//            }
//            cmd.call();
//        } catch (GitAPIException e){
//            throw die(e.getMessage(), e);
//        }
//    }

    @Option(name = "--staged", usage = "usage_restoreToIndex")
    private boolean toIndex = false;

    @Option(name = "--worktree", aliases = {"-w"},usage = "usage_toWorkTree")
    private boolean toWorkTree = false;

    @Option(name="--source", aliases = { "-s" }, usage="specifies the source")
    private String source;

    @Argument(required = true, metaVar = "metaVar_filepattern", usage = "usage_filesToAddContentFrom")
    private final List<String> filePatterns = new ArrayList<>();

    @Override
    protected void run() throws Exception{
        try (Git git = new Git(db)){
            RestoreCommand cmd = git.restore();
            if (!toIndex && !toWorkTree){
                toWorkTree = true;
            }

            cmd.setToIndex(toIndex).setToWorkTree(toWorkTree).setSource(source);

            for (String s:filePatterns){
                cmd.addFilePattern(s);
            }

            cmd.call();
        } catch (GitAPIException e){
            throw die(e.getMessage(), e);
        }
    }
}
