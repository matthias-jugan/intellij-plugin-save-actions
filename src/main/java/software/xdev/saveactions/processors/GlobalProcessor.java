package software.xdev.saveactions.processors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;

import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.codeInsight.actions.OptimizeImportsProcessor;
import com.intellij.codeInsight.actions.RearrangeCodeProcessor;
import com.intellij.codeInsight.actions.ReformatCodeProcessor;
import com.intellij.codeInsight.actions.VcsFacade;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.ChangedRangesInfo;

import software.xdev.saveactions.core.ExecutionMode;
import software.xdev.saveactions.core.filter.InspectionFilter;
import software.xdev.saveactions.core.filter.SmartVcsFilter;
import software.xdev.saveactions.model.Action;


/**
 * Available processors for global.
 */
@SuppressWarnings("java:S115")
public enum GlobalProcessor implements Processor
{
	organizeImports(Action.organizeImports, GlobalProcessor::optimizeImports),
	
	reformat(
		Action.reformat,
		(project, psiFiles) -> reformatCode(project, psiFiles, false)),
	
	reformatChangedCode(
		Action.reformatChangedCode,
		(project, psiFiles) -> reformatCode(project, psiFiles, true)),
	
	reformatChangedSurroundings(
		Action.reformatChangedSurroundings,
		(project, psiFiles) -> reformatSurroundings(psiFiles)),
	
	rearrange(Action.rearrange, GlobalProcessor::rearrangeCode);
	
	private static final Map<Action, GlobalProcessor> ACTION_VALUES = stream()
		.collect(Collectors.toMap(Processor::getAction, Function.identity()));
	
	@NotNull
	private static Runnable rearrangeCode(final Project project, final PsiFile[] psiFiles)
	{
		return new RearrangeCodeProcessor(
			project,
			psiFiles,
			CodeInsightBundle.message("command.rearrange.code"),
			null)::run;
	}
	
	@NotNull
	private static Runnable optimizeImports(final Project project, final PsiFile[] psiFiles)
	{
		return new OptimizeImportsProcessor(project, psiFiles, null)::run;
	}
	
	@NotNull
	private static Runnable reformatCode(
		final Project project,
		final PsiFile[] psiFiles,
		final boolean processChangedTextOnly)
	{
		return new ReformatCodeProcessor(project, psiFiles, null, processChangedTextOnly)::run;
	}
	
	@NotNull
	private static Runnable reformatSurroundings(final PsiFile[] psiFiles)
	{
		return () -> {
			for(final PsiFile psiFile : psiFiles)
			{
				final ChangedRangesInfo infos = VcsFacade.getInstance().getChangedRangesInfo(psiFile);
				if(infos == null)
				{
					continue;
				}
				final List<TextRange> changedRanges = new ArrayList<>(infos.allChangedRanges);
				if(infos.insertedRanges != null)
				{
					changedRanges.addAll(infos.insertedRanges);
				}
				final List<TextRange> upcastedRanges =
					changedRanges.stream().map(it -> SmartVcsFilter.expandRange(it, psiFile)).toList();
				final List<TextRange> mergedRanges = mergeRanges(upcastedRanges);
				new ReformatCodeProcessor(psiFile, mergedRanges.toArray(new TextRange[0])).run();
			}
		};
	}
	
	private static List<TextRange> mergeRanges(final Collection<TextRange> ranges)
	{
		final List<TextRange> mergedRanges = new ArrayList<>();
		for(final TextRange range : ranges)
		{
			TextRange mergedRange = range;
			final Iterator<TextRange> mergedRangesIterator = mergedRanges.iterator();
			while(mergedRangesIterator.hasNext())
			{
				final TextRange mr = mergedRangesIterator.next();
				if(mergedRange.intersects(mr))
				{
					mergedRangesIterator.remove();
					mergedRange = mergedRange.union(mr);
				}
			}
			mergedRanges.add(mergedRange);
		}
		return mergedRanges;
	}
	
	private final Action action;
	private final BiFunction<Project, PsiFile[], Runnable> command;
	
	GlobalProcessor(final Action action, final BiFunction<Project, PsiFile[], Runnable> command)
	{
		this.action = action;
		this.command = command;
	}
	
	@Override
	public Action getAction()
	{
		return this.action;
	}
	
	@Override
	public Set<ExecutionMode> getModes()
	{
		return EnumSet.allOf(ExecutionMode.class);
	}
	
	@Override
	public int getOrder()
	{
		return 3;
	}
	
	@Override
	public SaveWriteCommand getSaveCommand(
		final Project project,
		final Set<PsiFile> psiFiles,
		final InspectionFilter filter)
	{
		return new SaveWriteCommand(project, psiFiles, this.getModes(), this.getAction(), this.getCommand());
	}
	
	public BiFunction<Project, PsiFile[], Runnable> getCommand()
	{
		return this.command;
	}
	
	public static Optional<GlobalProcessor> getProcessorForAction(final Action action)
	{
		return Optional.ofNullable(ACTION_VALUES.get(action));
	}
	
	public static Stream<GlobalProcessor> stream()
	{
		return Arrays.stream(values());
	}
}
