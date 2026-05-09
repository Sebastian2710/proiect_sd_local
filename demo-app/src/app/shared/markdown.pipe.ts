import { Pipe, PipeTransform, inject } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { marked } from 'marked';

/**
 * Renders a Markdown string to sanitized HTML.
 *
 * <p>The LLM-generated project plan is the only thing we feed in here, but
 * we still go through {@link DomSanitizer} for defense-in-depth — a future
 * change might route user-supplied markdown through this pipe and we don't
 * want to silently introduce an XSS vector.
 */
@Pipe({ name: 'markdown' })
export class MarkdownPipe implements PipeTransform {
  private readonly sanitizer = inject(DomSanitizer);

  transform(value: string | null | undefined): SafeHtml {
    if (!value) {
      return '';
    }
    // marked.parse can be sync or async depending on the `async` option;
    // pinning async: false guarantees a plain string at runtime.
    const html = marked.parse(value, { async: false });
    return this.sanitizer.bypassSecurityTrustHtml(html);
  }
}
