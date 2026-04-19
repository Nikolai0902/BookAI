import ReactMarkdown from 'react-markdown'
import rehypeHighlight from 'rehype-highlight'
import 'highlight.js/styles/github-dark.css'

interface Props { answer: string }

export default function ReasonView({ answer }: Props) {
  return (
    <div className="prose prose-invert prose-sm max-w-none">
      <ReactMarkdown rehypePlugins={[rehypeHighlight]}>
        {answer}
      </ReactMarkdown>
    </div>
  )
}
