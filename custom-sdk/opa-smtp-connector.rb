{
  title: 'SMTP Connector V2',
  secure_tunnel: true,

  connection: {
    fields: [
      { 
        name: 'profile',
        label: 'Profile',
        hint: 'SMTP profile name configured on OPA',
        optional: false
      },
      { 
        name: 'authType',
        label: 'Authentication Type',
        control_type: 'select',
        pick_list: [
          ['STARTTLS (Default)', 'STARTTLS'],
          ['SSL', 'SSL'],
          ['TLS', 'TLS'],
          ['No Authentication', 'NONE']
        ],
        optional: false,
        default: 'STARTTLS',
        hint: 'Authentication type for SMTP server'
      },
      { 
        name: 'host',
        label: 'SMTP Host',
        hint: 'SMTP server hostname or IP address',
        optional: false
      },
      { 
        name: 'port',
        label: 'SMTP Port',
        type: 'integer',
        hint: 'SMTP server port number',
        optional: false
      },
      { 
        name: 'username',
        label: 'Username',
        hint: 'SMTP authentication username',
        optional: true,
        sticky: true
      },
      { 
        name: 'password',
        label: 'Password',
        hint: 'SMTP authentication password',
        optional: true,
        control_type: 'password',
        sticky: true
      },
      { 
        name: 'connectionTimeout',
        label: 'Connection Timeout',
        type: 'integer',
        optional: true,
        default: 10000,
        hint: 'Connection timeout in milliseconds (default: 10000)'
      },
      {
        name: 'timeout',
        label: 'Read Timeout',
        type: 'integer',
        optional: true,
        default: 10000,
        hint: 'Read timeout in milliseconds (default: 10000)'
      }
    ],
    authorization: { type: 'none' }
  },

  test: lambda do |connection|
    params = {
      host: connection['host'],
      port: connection['port'],
      authType: connection['authType'],
      username: connection['username'],
      password: connection['password']
    }
    
    # Add optional timeouts if specified
    params[:connectionTimeout] = connection['connectionTimeout'] if connection['connectionTimeout'].present?
    params[:timeout] = connection['timeout'] if connection['timeout'].present?
    
    response = post("http://localhost/ext/#{connection['profile']}/test", params)
      .headers('X-Workato-Connector': 'enforce')
    
    if response['status'] == 'error'
      error(response['message'])
    end
  end,

  actions: {
    sendEmail: {
      title: 'Send Email',
      subtitle: 'Send an email via SMTP server',
      description: 'Sends an email with optional attachments through configured SMTP server',
      
      input_fields: lambda do |object_definitions|
        object_definitions['email']
      end,

      execute: lambda do |connection, input|
        params = input.merge(
          host: connection['host'],
          port: connection['port'],
          authType: connection['authType'],
          username: connection['username'],
          password: connection['password']
        )
        
        # Add optional timeouts if specified
        params[:connectionTimeout] = connection['connectionTimeout'] if connection['connectionTimeout'].present?
        params[:timeout] = connection['timeout'] if connection['timeout'].present?
        
        response = post("http://localhost/ext/#{connection['profile']}/sendEmail", params)
          .headers('X-Workato-Connector': 'enforce')
        
        if response['status'] == 'error'
          error(response['message'])
        end
        
        response
      end,

      output_fields: lambda do |object_definitions|
        object_definitions['status']
      end
    }
  },

  pick_lists: {
    emailMimeType: lambda do |_connection|
      [
        ['Plain Text', 'text/plain; charset=UTF-8'],
        ['HTML', 'text/html; charset=UTF-8']
      ]
    end
  },

  object_definitions: {
    status: {
      fields: lambda do
        [
          {
            "control_type": "text",
            "label": "Status",
            "type": "string",
            "name": "status",
            "optional": false
          }
        ]
      end
    },

    email: {
      fields: lambda do
        [
          {
            name: 'from',
            label: 'From',
            optional: false,
            hint: 'Email address of the sender'
          },
          {
            name: 'recipients',
            label: 'Recipients',
            optional: false,
            hint: 'Comma-separated list of recipient email addresses'
          },
          {
            name: 'subject',
            label: 'Subject',
            optional: false
          },
          {
            name: 'emailMimeType',
            label: 'Email Format',
            control_type: 'select',
            pick_list: 'emailMimeType',
            optional: false,
            default: 'text/plain; charset=UTF-8'
          },
          {
            name: 'emailBody',
            label: 'Email Body',
            optional: false,
            control_type: 'text-area',
            hint: 'Content of the email. Can be plain text or HTML based on Email Format selection'
          },
          {
            name: 'attachments',
            label: 'Attachments',
            optional: true,
            type: 'array',
            of: 'object',
            properties: [
              {
                name: 'filename',
                label: 'Filename',
                optional: false
              },
              {
                name: 'content',
                label: 'Content (Base64)',
                optional: false,
                hint: 'Base64 encoded file content'
              },
              {
                name: 'contentType',
                label: 'Content Type',
                optional: false,
                hint: 'MIME type of the attachment'
              }
            ]
          }
        ]
      end
    }
  }
}